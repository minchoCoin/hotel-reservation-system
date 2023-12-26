package hotel_reservation_system;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import db_handler.HotelDBHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
/**
 * @brief 방의 체크인, 체크아웃을 관리하고 방 사용여부를 보여주는 클래스이다.
 */
public class HotelRoomManager {
	private HotelDBHandler hoteldbhandle;
	public HotelRoomManager(HotelDBHandler h1){
		hoteldbhandle=h1;
		
	}
	/**
	 * @brief		특정 날짜에 사용하는 방의 리스트를 보여준다.
	 * @param date	이 날짜에 사용되는 방의 리스트를 보여준다.
	 */
	public void printRoomOccupied(LocalDate date) {
		
		
		String sql=String.format("select reservationID,room_num,guestID,fname,lname,service_date from room_service natural join guest where service_date='%s';", date.toString());
		
		String[] header={"예약 ID","방 번호","손님 ID","fname","lname","날짜"};
		hoteldbhandle.processSelectPrint(sql, header);
	}
	/**
	 * @brief 			특정 날짜가 체크인 날짜인 방을 체크인한다.
	 * @detail			먼저 매개변수로 온 guestID와 체크인날짜를 이용하여 체크인날짜가 매개변수로 온 체크인날짜이면서 이 guest가 예약한 예약ID와 체크아웃날짜를 가져온다.
	 * 					예약ID와 체크아웃날짜를 순회하면서 이 예약ID로 예약된 방 번호를 가져오고, check_in 테이블에 관련 정보(예약ID, 방번호, guestID, 체크인날짜)를 insert한다.
	 * 					그리고 room_service 테이블에 해당 방을 체크인날짜부터 체크아웃날짜까지 사용할 것임을 기록한다.
	 * 					예를 들어 매개변수로 들어온 체크인 날짜가 2023-12-24라 하자. 이 guest가 예약하고, 체크인 날짜가 2023-12-24인 예약이 2개이고,
	 * 					한 예약은 301호 방을 2023-12-24일에 체크인, 2023-12-25일에 체크아웃하고,
	 * 					다른 예약은 201호 방을 2023-12-24일에 체크인, 2023-12-26일에 체크아웃이라면 room_reserve 테이블에 들어가는 정보는 다음과 같다.
	 * 					(예약1,301,guestID,2023-12-24),(예약2,201,guestID,2023-12-24),(예약2,201,guestID,2023-12-25)
	 * 					체크인 완료 후, additional_fee 테이블에 추가요금을 저장할 공간을 만든다. 즉, (예약ID,guestID,0)이 insert되고, guest가 이 예약ID로 물건을 구매(buy_something)할 수 있게 된다.
	 * 					체크인 완료 후, 체크인한 방 번호 리스트를 반환한다.
	 * @param guestID 	이 guest가 예약한 방을 체크인한다.
	 * @param current 	이 날짜가 체크인 날짜인 방을 체크인한다
	 * @return			체크인한 방 번호 리스트
	 */
	public List<Integer> checkIn(String guestID, LocalDate current) {
		String sql = String.format("select reservationID,checkOutDate from reservation where guestID='%s' and checkInDate='%s';",guestID,current.toString());
		List<String> reservationList = new ArrayList<String>();
		List<LocalDate> checkOutDateList = new ArrayList<LocalDate>();
		
		List<Integer> checkInList = new ArrayList<Integer>();
		
		ResultSet rs = hoteldbhandle.processSelect(sql);
		try {
			while(rs.next()) {
				reservationList.add(rs.getString(1));
				checkOutDateList.add(rs.getDate(2).toLocalDate());
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		for(int i=0;i<reservationList.size();++i) {
			List<Integer> roomList = new ArrayList<Integer>();
			if(hoteldbhandle.processSelectCount(String.format("select count(*) from check_in where reservationID='%s';", reservationList.get(i)))==0) {
				sql=String.format("select distinct room_num from rooms_reserve where reservationID=%s;", reservationList.get(i));
				ResultSet r = hoteldbhandle.processSelect(sql);
				try {
					while(r.next()) {
						int room_num = r.getInt(1);
						roomList.add(room_num);
					}
					r.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				for(int room_num:roomList) {
					sql=String.format("insert into check_in values (%s,%d,'%s','%s');", reservationList.get(i),room_num,guestID,current.toString());
					if(hoteldbhandle.processUpdate(sql)==1) {
						checkInList.add(room_num);
						
						LocalDate start = LocalDate.of(current.getYear(), current.getMonth(), current.getDayOfMonth());
						while(start.compareTo(checkOutDateList.get(i))<0) {
							sql=String.format("insert into room_service values(%s,%d,'%s','%s');",reservationList.get(i),room_num,guestID,start.toString());
							hoteldbhandle.processUpdate(sql);
							start=start.plusDays(1);
						}
					}
					
					
				}
				
				
				hoteldbhandle.processUpdate(String.format("insert into additional_fee values (%s,'%s',0);",reservationList.get(i),guestID ));
			}
		}
			
			
		return checkInList;
	}
	/**
	 * @brief 			특정 날짜가 체크아웃 날짜인 방을 체크아웃한다.
	 * @detail			매개변수로 받은 guestID와 날짜를 이용하여 해당 guestID가 예약한, 해당 날짜가 체크아웃날짜인 예약의 예약ID들을 가져온다.
	 * 					예약ID를 순회하면서 먼저 해당 예약ID가 체크인되어있는지 확인한다. 만약 체크인되어있지 않다면 체크인되어있지 않다는 메시지를 출력한다.
	 * 					체크인되어있다면 이 예약ID로 예약된 방 리스트를 가져오고, check_out 테이블에 관련 정보(예약ID, 방 번호,guestID, 체크아웃 날짜)를 insert한다.
	 * 					그리고 additional_fee 테이블을 이용하여 이 예약ID로 추가결제할 것이 있는지 확인한다.
	 * 					없다면 그대로 종료한다.
	 * 					만약 있다면, 추가결제할 요금을 안내하고, 결제수단을 입력받아 결제를 진행한다.
	 * @param guestID 	이 guestID를 가진 guest가 예약한 방을 체크아웃한다.
	 * @param current 	이 날짜가 체크아웃 날짜인 방을 체크아웃한다.
	 * @return
	 */
	public List<Integer> checkOut(String guestID, LocalDate current){
		String sql = String.format("select reservationID from reservation where guestID='%s' and checkOutDate='%s';",guestID,current.toString());
		List<String> reservationList = new ArrayList<String>();
		List<Integer> checkOutList = new ArrayList<Integer>();
		List<Integer> roomList = new ArrayList<Integer>();
		ResultSet rs = hoteldbhandle.processSelect(sql);
		try {
			while(rs.next()) {
				reservationList.add(rs.getString(1));
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i=0;i<reservationList.size();++i) {
			if(hoteldbhandle.processSelectCount(String.format("select count(*) from check_out where reservationID='%s';", reservationList.get(i)))==0) {
				sql = String.format("select count(*) from check_in where reservationID=%s", reservationList.get(i));
				int cnt = hoteldbhandle.processSelectCount(sql);
				
				if(cnt!=0) {
					sql=String.format("select distinct room_num from rooms_reserve where reservationID=%s;", reservationList.get(i));
					ResultSet r = hoteldbhandle.processSelect(sql);
					try {
						while(r.next()) {
							int room_num = r.getInt(1);
							roomList.add(room_num);
							
						}
						for(int room_num:roomList) {
							sql=String.format("insert into check_out values (%s,%d,'%s','%s')", reservationList.get(i),room_num,guestID,current.toString());
							if(hoteldbhandle.processUpdate(sql)==1) {
								checkOutList.add(room_num);
							}
						}
						//r.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					sql = String.format("select fee from additional_fee where reservationID = %s", reservationList.get(i));
					r = hoteldbhandle.processSelect(sql);
					try {
						if(r.next()) {
							int totalCost = r.getInt(1);
							if(totalCost>0) {
								HotelBillManager tmp = new HotelBillManager(hoteldbhandle);
								
								System.out.println("총 추가 요금은 " + Integer.toString(totalCost) + "입니다. 결제수단을 입력해주세요...");
								Scanner sc = new Scanner(System.in);
								String payment_method = sc.next();
								
								tmp.insertBillInfo(reservationList.get(i), guestID, totalCost, payment_method);
							}
							
						}
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					System.out.println(reservationList.get(i) + " : Not yet checkIn!");
				}
				
				
			}
			
		}
		return checkOutList;
	}
	/**
	 * @brief 			매개변수로 입력받은 체크인 날짜와 체크아웃 날짜를 이용하여 특정 방 타입의 예약할 수 있는 방 개수와 가격을 보여준다.
	 * @detail			먼저 매개변수로 받은 roomType을 검사한다. 만약 roomType 문자열이 비어있으면, rooms_type 테이블에서 방 타입 리스트를 가져와
	 * 					전체 방 타입의 예약가능 개수와 가격을 보여주는데 사용된다. roomType 문자열이 비어있지 않으면,(roomTypeList에 이 방 타입을 넣고) 이 방 타입의 방을 보여준다.
	 * 					검색할 방 타입 리스트를 순회하면서, 먼저 예약가능 개수를 계산한다. 이는 해당 방 타입의 방 개수에서 매개변수로 입력받은 체크인날짜~체크아웃날짜 구간에 겹치는 해당 방 타입의 예약이 예약한 방 개수를 뺴서 구한다.
	 * 					만약 예약가능 개수가 0이면, 해당 방 타입을 출력하지 않는다.
	 * 					이제 가격을 계산한다. 가격은 체크인날짜부터 체크아웃날짜 하루 전까지 순회하면서 해당 날짜의 가격을 더하여 계산한다. 만약 해당 날짜가 special_price 테이블에 특별 가격 날짜시작~끝 구간 내에 있으면 특별 가격을 총 가격에 더한다.
	 * 					만약 그렇지 않다면, 기본 가격을 더한다.
	 * 					계산한 예약가능한 방 개수와 가격을 이용하여 예약가능한 방 타입과 싱글침대 개수, 더블침대 개수, 예약가능 개수, 방 가격을 보여준다.
	 * @param start 	이 날짜에 체크인할 수 있는 방 종류을 보여준다.
	 * @param end		이 날짜에 체크아웃할 수 있는 방 종류를 보여준다.
	 * @param roomType	이 방 타입의 방 개수와 가격을 보여준다. 비어있을 경우, 모든 방 타입의 방 개수와 가격을 보여준다.
	 */
	public void printAvailableRoomTypeList(LocalDate start, LocalDate end, String roomType) {
		List<String> roomTypeList = new ArrayList<>();
		List<Integer> roomTypecnt = new ArrayList<>();
		List<Integer> roomReservationCnt =new ArrayList<>();
		List<Integer> totalPriceList = new ArrayList<>();
		if(roomType.isEmpty()) {
			String sql = "select name from rooms_type;";
			ResultSet rs = hoteldbhandle.processSelect(sql);
			try {
				while(rs.next()) {
					roomTypeList.add(rs.getString(1));
				}
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			roomTypeList.add(roomType);
		}
		
		for(String rt:roomTypeList) {
			roomTypecnt.add(hoteldbhandle.processSelectCount(String.format("select count(*) from rooms where room_type='%s';", rt)));
			roomReservationCnt.add(hoteldbhandle.processSelectCount(String.format("select sum(cnt) from reservation natural join rooms_type_reserve where room_type='%s' and (checkInDate>='%s' and checkInDate<'%s' or checkOutDate>'%s' and checkOutDate<='%s');", rt,start.toString(),end.toString(),start.toString(),end.toString())));
		}
		int availableCnt=0;
		int price=0;
		int special_price=0;
		LocalDate specialStart;
		LocalDate specialEnd;
		for(int i=0;i<roomTypeList.size();++i) {
			if(roomTypecnt.get(i)>roomReservationCnt.get(i)) {
				availableCnt++;
				ResultSet rs = hoteldbhandle.processSelect(String.format("select price from rooms_type where name='%s'", roomTypeList.get(i)));
				try {
					if(rs.next()) {
						price=rs.getInt(1);
					}
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int tmpPrice=0;
				rs = hoteldbhandle.processSelect(String.format("select start_date, end_date,price from special_price where room_type='%s';", roomTypeList.get(i)));
				try {
					if(rs.next()) {
						specialStart = rs.getDate(1).toLocalDate();
						specialEnd = rs.getDate(2).toLocalDate();
						special_price = rs.getInt(3);
						
						
						for(LocalDate tmp = LocalDate.of(start.getYear(), start.getMonth(), start.getDayOfMonth());tmp.compareTo(end)<0;tmp=tmp.plusDays(1)){
							if(tmp.compareTo(specialStart)>=0 && tmp.compareTo(specialEnd)<0) {
								tmpPrice +=special_price;
								
							}
							else {
								tmpPrice +=price;
							}
						}
						
						totalPriceList.add(tmpPrice);
					}
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String[] header= {"방 이름","싱글침대 개수","더블침대 개수","남은 방 개수","총 가격"};
				hoteldbhandle.processSelectPrint(String.format("select name, single_bed_cnt,double_bed_cnt, %d as available, %d as total_price from rooms_type where name='%s';", roomTypecnt.get(i)-roomReservationCnt.get(i),tmpPrice,roomTypeList.get(i)),header );
			}
		}
		if(availableCnt==0) {
			System.out.println("No Available Rooms!");
		}
	}
}
