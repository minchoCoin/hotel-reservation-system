package hotel_client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Scanner;

import db_handler.HotelDBHandler;
import hotel_reservation_system.HotelAdminManager;
import hotel_reservation_system.HotelBillManager;
import hotel_reservation_system.HotelHouseKeepManager;
import hotel_reservation_system.HotelReservationManager;
import hotel_reservation_system.HotelRoomManager;

/**
 * @brief 호텔 manager가 입력한 명령을 받아 실행한다.
 */
public class HotelAdminClient {
	/**
	 * @brief 호텔 manager가 입력한 명령을 받아 실행한다.
	 * @detail 수행할 수 있는 명령은 다음과 같다.
	 * 			rooms_occupied		현재 손님들이 사용하고 있는 방의 리스트를 볼 수 있다.
	 * 			reservationList 	오늘 이후로 예약되어있는 예약 리스트를 볼 수 있다.
	 * 			rooms_reserve 		특정 날짜 이후로 사용할 예정인 방 리스트를 볼 수 있다.
	 * 			housekeeping 		특정 날짜 이후로 할당되어있는 housekeeping 리스트를 볼 수 있다.
	 * 			change_housekeeping 특정 날짜에 특정 방에 할당된 housekeeping 완료 여부를 변경할 수 있다.
	 * 			billList			특정 날짜 이상, 특정 날짜 이하에 결제된 내역을 볼 수 있다.
	 * 			help				사용할 수 있는 명령을 출력한다.
	 * 			exit				manager 모드를 종료한다.
	 * @param hoteldbhandle  hotel 데이터베이스에 연결되어있는 HotelDBHandler
	 * @param adminManage manager로 로그인한 세션이 있는 HotelAdminManager
	 * @param sc Scanner
	 */
	public static void startTransaction(HotelDBHandler hoteldbhandle, HotelAdminManager adminManage,Scanner sc) {
		String id = adminManage.getManagerID();
		
		String commandList = "rooms_occupied, reservationList, housekeeping, rooms_reserve,change_housekeeping,  billList, exit, help";
		SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");
		System.out.println(commandList);
		HotelRoomManager hotelRoomManage = new HotelRoomManager(hoteldbhandle);
		HotelHouseKeepManager hotelHouseKeepManage = new HotelHouseKeepManager(hoteldbhandle);
		HotelReservationManager hotelReservationManage = new HotelReservationManager(hoteldbhandle);
		HotelBillManager hotelBillManage = new HotelBillManager(hoteldbhandle);
		while(true) {
			System.out.print(id + "# ");
			String operation = sc.next();
			
			operation = operation.toLowerCase();
			
			if(operation.compareTo("rooms_occupied")==0) {
				hotelRoomManage.printRoomOccupied(LocalDate.now());
			}
			else if(operation.compareTo("reservationlist")==0) {
				hotelReservationManage.show_reservations(LocalDate.now());
			}
			else if(operation.compareTo("rooms_reserve")==0){
				System.out.print("Enter date: ");
				String date = sc.next();
				
				try {
					hotelReservationManage.show_room_reserve(transFormat.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(operation.compareTo("housekeeping")==0) {
				LocalDate date;
				try {
					System.out.print("Enter date : ");
					date = transFormat.parse(sc.next()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					hotelHouseKeepManage.showHouseKeep(date);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else if(operation.compareTo("change_housekeeping")==0) {
				System.out.print("Enter room_num: ");
				int num = sc.nextInt();
				System.out.print("Enter date : ");
				String date = sc.next();
				System.out.print("Enter status : complete or incomplete : ");
				String status;
				do {
					status=sc.next();
					if(status.compareTo("complete")!=0 && status.compareTo("incomplete")!=0) {
						System.out.print("Enter status : complete or incomplete : ");
					}
				}while(status.compareTo("complete")!=0 && status.compareTo("incomplete")!=0);
				
				try {
					hotelHouseKeepManage.changeHouseKeep(num,transFormat.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() , status);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(operation.compareTo("billlist")==0) {
				System.out.print("Enter start_date : ");
				String start = sc.next();
				System.out.print("Enter end_date : ");
				String end = sc.next();
				try {
					hotelBillManage.billList(transFormat.parse(start).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), transFormat.parse(end).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(operation.compareTo("exit")==0) {
				adminManage.logout();
				break;
			}
			else if(operation.compareTo("help")==0) {
				System.out.println(commandList);
			}
			else {
				System.out.println("Invalid Command!");
			}
		}
		
	}
}
