package hotel_reservation_system;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import db_handler.HotelDBHandler;
/**
 * @brief 예약, 예약취소, 예약 내역을 보여주거나 예약된 방을 보여주는 클래스이다.
 */
public class HotelReservationManager {
	private HotelDBHandler hoteldbhandle;
	
	
	public HotelReservationManager(HotelDBHandler h1) {
		hoteldbhandle=h1;
		
	}
	/**
	 * @brief 	방을 예약한다.
	 * @detail 	먼저 매개변수로 받은 방 타입에 해당하는 방의 개수를 구한다. 그리고 체크인날짜~체크아웃날짜 구간에 겹치는 방 타입 예약 개수를 구한다.
	 * 			이 값의 차이, 즉 room_type 방 타입의 예약가능한 방의 개수가 예약하려는 방의 개수보다 적으면 -1을 반환한다.
	 * 			만약 예약가능한 방의 개수가 더 많으면 예약을 진행한다. 먼저 가격을 구한다. 체크인날짜부터 시작하여 체크아웃날짜 하루전까지 순회하면서
	 * 			special_price를 받아야하는 날짜인지 검사하고, 맞으면 special_price 테이블에 있는 가격으로, 아니면 일반 가격을 더하여
	 * 			방 하나당 가격을 산정하고 여기에 방 개수를 곱하여 최종 가격을 산출한다.
	 * 			먼저 reservation 테이블에 예약 정보를 넣고(예약ID는 hoteldbhandle.getNewID()로 얻음), 결제 여부와 결제 수단을 입력받아 결제를 진행(bill 테이블에 정보 insert)한다.
	 * 			만약 결제를 안하겠다고 하면 reservation 테이블에서 예약 내역을 삭제한다.
	 * 			결제를 하겠다고 하면 결제 수단을 입력받아 결제를 수행한다.
	 * 			그리고 rooms_type_reserve에 예약하려는 방 종류와 개수를 insert한다. 예를 들어 double room 2개를 예약할 시 (예약ID, 'double room', 2)가 들어간다.
	 * 			그리고 예약할 방 번호를 검색한다. 이는 예약하려는 방 타입의 방 번호 리스트에서 체크인날짜부터 체크아웃날짜 하루전까지 예약된 방을 except해서 구한다.
	 * 			이제 rooms_reserve에 방 번호와 사용할 날짜를 insert한다. 예를 들어 체크인 날짜가 2023-12-24, 체크아웃 날짜가 2023-12-26이면서 301호를 예약한다면 테이블에 (예약 ID, 301,2023-12-24),(예약 ID, 301,2023-12-25)가 삽입된다.
	 * 			만약 예약할 수 있는 방이 없거나 rooms_reserve에 insert하는 도중에 방이 다 차서 insert에 실패하면 insert한 내역을 지우고 -1을 반환한다.
	 * 			rooms_reserve 테이블에 방 번호 예약 정보까지 다 insert했다면 housekeep 내역을 할당하고 예약 ID를 반환한다.
	 * @param guestID 방을 예약하려는 guest의 ID
	 * @param checkInDate 체크인 날짜
	 * @param checkOutDate 체크아웃 날짜
	 * @param room_type 방 타입(방 종류)
	 * @param cnt 방 개수
	 * @return 예약에 성공할 시 예약 ID, 실패할 시 -1 반환
	 */
	public BigDecimal reservation(String guestID,LocalDate checkInDate, LocalDate checkOutDate,String room_type, int cnt) {
		int totalCnt=hoteldbhandle.processSelectCount(String.format("select count(*) from rooms where room_type='%s';", room_type));
		int occupiedCnt = hoteldbhandle.processSelectCount(String.format("select sum(cnt) from reservation natural join rooms_type_reserve where room_type='%s' and (checkInDate>='%s' and checkInDate<'%s' or checkOutDate>'%s' and checkOutDate<='%s');", room_type,checkInDate.toString(),checkOutDate.toString(),checkInDate.toString(),checkOutDate.toString()));
		
		if(cnt>totalCnt - occupiedCnt) return new BigDecimal("-1");
		if(totalCnt==0) return new BigDecimal("-1");
		
		int price=0;
		LocalDate specialStart;
		LocalDate specialEnd;
		int special_price;
		int total_price=0;
		
		List<Integer> roomList = new ArrayList<>();
		
		ResultSet rs = hoteldbhandle.processSelect(String.format("select price from rooms_type where name='%s'", room_type));
		try {
			if(rs.next()) {
				price=rs.getInt(1);
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		rs = hoteldbhandle.processSelect(String.format("select start_date, end_date,price from special_price where room_type='%s';", room_type));
		try {
			if(rs.next()) {
				specialStart = rs.getDate(1).toLocalDate();
				specialEnd = rs.getDate(2).toLocalDate();
				special_price = rs.getInt(3);
				
				
				for(LocalDate tmp = LocalDate.of(checkInDate.getYear(), checkInDate.getMonth(), checkInDate.getDayOfMonth());tmp.compareTo(checkOutDate)<0;tmp=tmp.plusDays(1)){
					if(tmp.compareTo(specialStart)>=0 && tmp.compareTo(specialEnd)<0) {
						total_price +=special_price;
						
					}
					else {
						total_price +=price;
					}
				}
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		total_price*=cnt;
		
		BigDecimal reserveID = hoteldbhandle.getNewID("reservation", "reservationID");
		BigDecimal reserveID2 = hoteldbhandle.getNewID("reserve_cancel", "reservationID");
		if(reserveID.compareTo(reserveID2)<0) reserveID=reserveID2;
		hoteldbhandle.processUpdate(String.format("insert into reservation values (%s,'%s','%s','%s',%d);", reserveID.toString(),guestID,checkInDate.toString(),checkOutDate.toString(),total_price));
		
		System.out.print(Integer.toString(total_price)+"원 : 결제하시겠습니까? Y or N : ");
		Scanner sc = new Scanner(System.in);
		String answer = sc.next();
		if(answer.compareTo("Y")==0|| answer.compareTo("y")==0) {
			
			System.out.print("결제수단 입력 : ");
			sc.nextLine();
			String pay = sc.nextLine();
			
			HotelBillManager hotelBillManage = new HotelBillManager(hoteldbhandle);
			hotelBillManage.insertBillInfo(reserveID.toString(), guestID, total_price, pay);
			
			hoteldbhandle.processUpdate(String.format("insert into rooms_type_reserve values (%s,'%s',%d);", reserveID.toString(),room_type,cnt));
			
			for(int j=0;j<cnt;++j) {
				rs = hoteldbhandle.processSelect(String.format("select room_num from rooms where room_type='%s' except select distinct room_num from rooms_reserve where reserve_date>='%s' and reserve_date<'%s';", room_type,checkInDate.toString(),checkOutDate.toString()));
				try {
					if(rs.next()) {
						int room_num = rs.getInt(1);
						roomList.add(room_num);
						LocalDate start = LocalDate.of(checkInDate.getYear(), checkInDate.getMonth(), checkInDate.getDayOfMonth());
						while(start.compareTo(checkOutDate)<0) {
							String sql=String.format("insert into rooms_reserve values(%s,'%s','%s')",reserveID,room_num,start.toString());
							if(hoteldbhandle.processUpdate(sql)<1) {
								System.out.println("No Room!");
								hoteldbhandle.processUpdate(String.format("delete from rooms_reserve where reservationID='%s';", reserveID));
								hoteldbhandle.processUpdate(String.format("delete from rooms_type_reserve where reservationID='%s'", reserveID));
								hoteldbhandle.processUpdate(String.format("delete from reservation where reservationID='%s';", reserveID));
								rs.close();
								return new BigDecimal("-1");
							}
							start=start.plusDays(1);
						}
					}
					else {
						System.out.println("No Room!");
						hoteldbhandle.processUpdate(String.format("delete from rooms_reserve where reservationID='%s';", reserveID));
						hoteldbhandle.processUpdate(String.format("delete from rooms_type_reserve where reservationID='%s';", reserveID));
						hoteldbhandle.processUpdate(String.format("delete from reservation where reservationID='%s';", reserveID));
						rs.close();
						return new BigDecimal("-1");
					}
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			HotelHouseKeepManager hotelHouseKeepManage = new HotelHouseKeepManager(hoteldbhandle);
			
			for(int num:roomList) {
				hotelHouseKeepManage.insertHouseKeep(num, checkInDate, checkOutDate);
			}
			
			
			
		}
		else {
			hoteldbhandle.processUpdate(String.format("delete from reservation where reservationID=%s", reserveID.toString()));
			System.out.println("예약이 취소되었습니다.");
			return new BigDecimal("-1");
		}
		
		return reserveID;
	}
	/**
	 * @brief 				예약을 취소한다.
	 * @detail 				매개변수로 입력받은 예약ID와 guestID에 해당하는 예약을 취소한다.
	 * 						만약 예약ID와 guestID에 해당하는 예약이 없으면 -1을 리턴한다.
	 * 						해당하는 예약이 있으면 먼저 해당 예약의 체크인날짜와 체크아웃 날짜를 확인한다. 만약 체크인날짜보다 예약을 취소하는 날짜가 같거나 더 이후이면 취소를 거부한다.
	 * 						체크인날짜보다 예약을 취소하는 날짜가 더 이전이면 이 예약과 관련된 방의 housekeep 내역을 삭제한다.
	 * 						그리고 이 예약과 관련된 rooms_type_reserve, rooms_reserve, reservation 테이블에 항목을 삭제한다.
	 * 						reservation_cancel에 취소된 예약의 정보를 insert한다.
	 * @param reservationID 취소하려는 예약의 ID
	 * @param guestID 		이 예약을 취소하려는 guest의 ID
	 * @return
	 */
	public int cancel_reservation(String reservationID, String guestID) {
		
		int cnt = hoteldbhandle.processSelectCount(String.format("select count(*) from reservation where reservationID=%s and guestID='%s';", reservationID,guestID));
		if(cnt==0) return -1;
		
		List<Integer> roomList = new ArrayList<>();
		ResultSet rs = hoteldbhandle.processSelect(String.format("select * from reservation where reservationID=%s;",reservationID ));
		try {
			HotelHouseKeepManager hotelHouseKeepManage = new HotelHouseKeepManager(hoteldbhandle);
			if(rs.next()) {
				LocalDate checkInDate = rs.getDate("checkInDate").toLocalDate();
				LocalDate checkOutDate = rs.getDate("CheckOutDate").toLocalDate();
				int totalCost = rs.getInt("totalCost");
				LocalDate cancelDate = LocalDate.now();
				
				if(checkInDate.compareTo(cancelDate)<=0) {
					System.out.println("you can't cancel reservation that checkInDate is today");
				}
				else {
					
					ResultSet r = hoteldbhandle.processSelect(String.format("select room_num from rooms_reserve where reservationID=%s", reservationID));
					
					while(r.next()) {
						int num = r.getInt(1);
						roomList.add(num);
						
					}
					r.close();
					
					
					
					hoteldbhandle.processUpdate(String.format("delete from rooms_reserve where reservationID=%s;", reservationID));
					hoteldbhandle.processUpdate(String.format("delete from rooms_type_reserve where reservationID=%s", reservationID));
					hoteldbhandle.processUpdate(String.format("delete from reservation where reservationID=%s", reservationID));
					
					hoteldbhandle.processUpdate(String.format("insert into reserve_cancel values(%s,'%s','%s','%s','%s',%d)",reservationID,guestID,cancelDate.toString(),checkInDate.toString(),checkOutDate.toString(),totalCost));
					
					for(int num:roomList) {
						hotelHouseKeepManage.deleteHouseKeep(num, checkInDate, checkOutDate);
					}
					
					HotelBillManager hotelBillManage = new HotelBillManager(hoteldbhandle);
					
					hotelBillManage.cancelBill(reservationID, guestID);
					rs.close();
					return totalCost;
				}
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	/**
	 * @brief 특정 날짜 또는 그 이후가 체크아웃날짜인 예약 리스트를 보여준다.
	 * @param today 이 날짜 또는 그 이후가 체크아웃날짜인 예약 리스트를 보여준다.
	 */
	public void show_reservations(LocalDate today) {
		String sql = String.format("select * from reservation natural join rooms_type_reserve where checkOutDate>='%s'",today.toString());
		String[] header = {"예약 ID","손님 ID","체크인 날짜","체크아웃 날짜","총 요금","방 종류","개수"};
		hoteldbhandle.processSelectPrint(sql,header);
	}
	/**
	 * @brief 특정 날짜 또는 그 이후에 사용예정인 방 번호를 보여준다.
	 * @param today 이 날짜 또는 그 이후에 사용예정인 방 번호를 보여준다.
	 */
	public void show_room_reserve(LocalDate today) {
		String sql = String.format("select reservationID, guestID,room_num,reserve_date from reservation natural join rooms_reserve where reserve_date>='%s'", today.toString());
		String[] header = {"예약 ID","손님 ID","방 번호","사용예정날짜"};
		hoteldbhandle.processSelectPrint(sql, header);
	}
}
