package hotel_client;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

import db_handler.HotelDBHandler;
import hotel_reservation_system.HotelGuestManager;
import hotel_reservation_system.HotelReservationManager;
import hotel_reservation_system.HotelRoomManager;
/**
 * @brief guest의 명령을 입력받아 실행한다.
 */
public class HotelGuestClient {
	/**
	 * @brief guest의 명령을 입력받아 실행한다.
	 * @detail 사용할 수 있는 명령은 다음과 같다.
	 * 			rooms_available		체크인 날짜, 체크아웃 날짜, 방 종류(all 입력시 전부 조회)를 입력받아 가능한 방 종류 리스트를 보여준다.
	 * 			reserve 			체크인 날짜, 체크아웃 날짜, 방 종류, 개수를 입력받아 예약하고 결제한다.
	 * 			reserve_cancel 		예약ID를 입력받아 해당 예약을 취소한다. 단, 해당 예약 ID로 예약한 guest가 이 명령을 호출한 guest가 아닐 경우 거부
	 * 			my_reservations		이 guest가 특정 날짜 이후로 예약한(즉, 체크인 날짜가 특정 날짜 이후인) 예약 리스트를 보여준다.
	 * 			check_in			날짜를 입력받아 해당 날짜가 체크인 날짜이면서 이 guest가 예약한 예약에 할당된 방을 체크인한다.
	 * 			check_out			날짜를 입력받아 해당 날짜가 체크아웃 날짜이면서 이 guest가 예약한 예약에 할당된 방을 체크아웃한다(체크인 하지 않은 경우 거부)
	 * 			cost_at_checkout	예약ID를 입력받아 체크인 이후로 발생된 추가 요금을 확인한다.
	 * 			buy_something		예약ID와 살 물건을 입력받아 물건을 구매한다. 물건 가격은 체크아웃이 한꺼번에 결제한다.
	 * 			help				guest가 사용할 수 있는 명령을 출력한다.
	 * 			exit				guest 모드를 종료한다.
	 * @param hoteldbhandle hotel 데이터베이스에 연결된 HotelDBHandler
	 * @param guestManage guest로 로그인한 세션이 들어있는 HotelGuestManager
	 * @param sc Scanner
	 */
	public static void startTransaction(HotelDBHandler hoteldbhandle,HotelGuestManager guestManage,Scanner sc) {
		String id = guestManage.getGuestID();
		System.out.println("명령어 : rooms_available, reserve,reserve_cancel, my_reservations, cost_at_checkout, check_in, check_out,buy_something,help,exit");
		
		SimpleDateFormat transFormat =new SimpleDateFormat("yyyy-MM-dd");
		HotelRoomManager hotelRoomManage = new HotelRoomManager(hoteldbhandle);
		HotelReservationManager hotelReservationManage = new HotelReservationManager(hoteldbhandle);
		while(true) {
			System.out.print(id + ">> ");
			String operation = sc.next();
			
			operation = operation.toLowerCase();
			
			if(operation.compareTo("rooms_available")==0) {
				LocalDate checkIn =LocalDate.now();
				LocalDate checkOut = LocalDate.now();
				try {
					System.out.print("Enter checkInDate : ");
					checkIn = transFormat.parse(sc.next()).toInstant().atZone(ZoneId.systemDefault())
						      .toLocalDate();
					System.out.print("Enter checkOutDate : ");
					checkOut = transFormat.parse(sc.next()).toInstant().atZone(ZoneId.systemDefault())
						      .toLocalDate();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.print("Enter roomType(all for all): ");
				sc.nextLine();
				String room_type = sc.nextLine();
				
				if(room_type.compareTo("all")==0) {
					room_type = new String("");
				}
				hotelRoomManage.printAvailableRoomTypeList(checkIn, checkOut, room_type);
			}
			else if(operation.compareTo("reserve")==0) {
				
				LocalDate checkIn =LocalDate.now();
				LocalDate checkOut = LocalDate.now();
				try {
					System.out.print("Enter checkInDate : ");
					checkIn = transFormat.parse(sc.next()).toInstant().atZone(ZoneId.systemDefault())
						      .toLocalDate();
					System.out.print("Enter checkOutDate : ");
					checkOut = transFormat.parse(sc.next()).toInstant().atZone(ZoneId.systemDefault())
						      .toLocalDate();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.print("Enter roomType : ");
				sc.nextLine();
				String room_type = sc.nextLine();
				
				System.out.print("Enter count : ");
				int cnt = sc.nextInt();
				
				BigDecimal reservationID = hotelReservationManage.reservation(id, checkIn, checkOut, room_type, cnt);
				if(reservationID.compareTo(new BigDecimal("0"))<0) {
					System.out.println("reservation Failed");
				}
				else {
					System.out.println("reservation complete! your reservationID is " + reservationID.toString());
				}
			}
			
			else if(operation.compareTo("reserve_cancel")==0) {
				System.out.print("Enter your reservationID : ");
				String reserveID = sc.next();
				int price = hotelReservationManage.cancel_reservation(reserveID, id);
				if(price>=0) {
					System.out.println("reservationID : "+reserveID + ", totalCost : "+Integer.toString(price) + " is cancelled!");
				}
				else {
					System.out.println("cancel failed");
				}
			}
			
			else if(operation.compareTo("my_reservations")==0) {
				guestManage.printReservationList(LocalDate.now());
			}
			else if(operation.compareTo("cost_at_checkout")==0) {
				System.out.print("Enter reservation number : ");
				String reserveID = sc.next();
				String sql = String.format("select fee from additional_fee where reservationID=%s and guestID = '%s'", reserveID,id);
				ResultSet rs = hoteldbhandle.processSelect(sql);
				try {
					if(rs.next()) {
						int totalCost = rs.getInt(1);
						System.out.println("totalCost : " + Integer.toString(totalCost));
					}
					else {
						System.out.println("not yet checkin or invalid reservationID!");
					}
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else if(operation.compareTo("check_in")==0) {
				System.out.print("Enter checkInDate : ");
				LocalDate checkIn=LocalDate.now();
				try {
					checkIn = transFormat.parse(sc.next()).toInstant().atZone(ZoneId.systemDefault())
						      .toLocalDate();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				List<Integer> roomList = hotelRoomManage.checkIn(id,checkIn);
				System.out.print("CheckInList : ");
				for(int room:roomList) {
					System.out.print(Integer.toString(room) + " ");
				}
				System.out.println();
			}
			else if(operation.compareTo("check_out")==0) {
				System.out.print("Enter checkOutDate : ");
				LocalDate checkOut = LocalDate.now();
				try {
					checkOut = transFormat.parse(sc.next()).toInstant().atZone(ZoneId.systemDefault())
						      .toLocalDate();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				List<Integer> roomList = hotelRoomManage.checkOut(id,checkOut);
				System.out.print("CheckOutList : ");
				for(int room:roomList) {
					System.out.print(Integer.toString(room) + " ");
				}
				System.out.println();
			}
			else if(operation.compareTo("help")==0) {
				System.out.println("명령어 : rooms_available, reserve, reserve_cancel my_reservations, cost_at_checkout, check_in, check_out,buy_something,help,exit");
			}
			else if(operation.compareTo("exit")==0) {
				guestManage.logout();
				break;
			}
			else if(operation.compareTo("buy_something")==0) {
				System.out.println("1: water(1000 won), 2: juice(3000 won), 3: alcohol(10000 won)");
				int select = sc.nextInt();
				
				System.out.print("Enter your reservationID: ");
				String reservationID = sc.next();
				
				ResultSet rs = hoteldbhandle.processSelect(String.format("select fee from additional_fee where reservationID = %s and guestID = '%s'",reservationID,id));
				try {
					int cost = 0;
					if(rs.next()) {
						cost=rs.getInt(1);
					}
					
					else {
						System.out.println("Invalid reservationID or not yet check_in.");
						rs.close();
					}
					
					if(select==1) {
						cost+=1000;
					}
					else if(select==2) {
						cost+=3000;
					}
					else if(select==3) {
						cost+=10000;
					}
					else {
						System.out.println("Invalid buy_something input");
						rs.close();
						return;
					}
					
					if(hoteldbhandle.processUpdate(String.format("update additional_fee set fee=%d where reservationID=%s and guestID='%s'",cost,reservationID,id))==1) {
						System.out.println("구매 완료!");
					}
					
					rs.close();
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Invalid Command!");
			}
			
		}
		
	}
}
