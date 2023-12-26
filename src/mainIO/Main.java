package mainIO;

import java.io.Console;
import java.util.InputMismatchException;
import java.util.Scanner;

import db_handler.HotelDBHandler;
import hotel_client.HotelAdminClient;
import hotel_client.HotelGuestClient;
import hotel_reservation_system.HotelAdminManager;
import hotel_reservation_system.HotelGuestManager;
/**
 * @brief main 클래스
 */
public class Main {
	/**
	 * @brief guest나 manager로 로그인하여 각종 명령을 수행하거나 guest 회원가입을 진행한다.
	 * @param args
	 */
	public static void main(String[] args) {
		HotelDBHandler hoteldbhandle = new HotelDBHandler();
		hoteldbhandle.connect();
		
		Scanner sc = new Scanner(System.in);
		while(true) {
			
			System.out.println("Guest OR Manager?");
			System.out.println("Guest : 1, Manager : 2, Register(for guest): 3...Exit : any key except 1 or 2 or 3");
			int loginType;
			try {
				loginType = sc.nextInt();
			}catch(InputMismatchException e) {
				loginType=10;
			}
			
			
			if(loginType==1) {
				HotelGuestManager hotelGuestManage = new HotelGuestManager(hoteldbhandle);
				System.out.print("id : ");
				String id = sc.next();
				System.out.print("pwd : ");
				String pwd = sc.next();
				
				if(hotelGuestManage.login(id, pwd)) {
					HotelGuestClient.startTransaction(hoteldbhandle,hotelGuestManage,sc);
				}
				else {
					System.out.println("id and password no match!!");
				}
				
			}
			else if(loginType==2) {
				HotelAdminManager hotelAdminManage = new HotelAdminManager(hoteldbhandle);
				System.out.print("id : ");
				String id = sc.next();
				System.out.print("pwd : ");
				String pwd = sc.next();
				
				if(hotelAdminManage.login(id, pwd)) {
					HotelAdminClient.startTransaction(hoteldbhandle,hotelAdminManage,sc);
				}
				else {
					System.out.println("id and password no match!!");
				}
			}
			
			else if(loginType==3) {
				HotelGuestManager hotelGuestManage = new HotelGuestManager(hoteldbhandle);
				System.out.println("Enter your information : ");
				System.out.print("id : ");
				String id = sc.next();
				System.out.print("pwd : ");
				String pwd = sc.next();
				System.out.print("fname : ");
				String fname = sc.next();
				System.out.print("lname : ");
				String lname = sc.next();
				System.out.print("email : ");
				String email = sc.next();
				if(!hotelGuestManage.register(id, pwd, fname, lname, email)) {
					System.out.println("ID duplicated! register failed");
				}
			}
			
			else {
				hoteldbhandle.disconnect();
				break;
			}
		}
		
	}
}
