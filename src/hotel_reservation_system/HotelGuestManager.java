package hotel_reservation_system;
import java.security.NoSuchAlgorithmException;

import encryption.SHA256;

import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import db_handler.HotelDBHandler;
/**
 * @brief guest로 로그인 및 로그아웃, 회원가입을 수행하는 클래스이다.
 */
public class HotelGuestManager {
	private HotelDBHandler hoteldbhandle;
	private SHA256 sha256=null;
	private String guestID=null;
	/**
	 * @brief 	로그인된 guest의 guestID를 반환한다.
	 * @return 	로그인된 guest의 guestID
	 */
	public String getGuestID() {
		return guestID;
	}
	
	public HotelGuestManager(HotelDBHandler h1) {
		hoteldbhandle=h1;
		sha256=new SHA256();
		
	}
	/**
	 * @brief 	입력받은 아이디와 패스워드로 로그인한다.
	 * @detail 	guest 테이블에 일치하는 아이디와 비밀번호가 있는지 확인하여 있으면 로그인성공, 없으면 로그인실패이다.
	 * @param 	id 아이디
	 * @param 	pwd 비밀번호
	 * @return 	로그인성공여부 로그인성공시 true를 반환한다.
	 */
	public boolean login(String id, String pwd) {
		if(guestID!=null) {
			System.out.println("already login!");
			return false;
		}
		int cnt=0;
		try {
			
			String sql=String.format("select count(*) from guest where guestID='%s' and pwd='%s';", id,sha256.encrypt(pwd));
			cnt=hoteldbhandle.processSelectCount(sql);
			if(cnt==1) {
				guestID=id;
				System.out.println("Welcome " + id);		
			}
		}catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return cnt==1;
			
	}
	/**
	 * @brief 		회원가입을 진행한다. id 중복 여부를 검사하고, 중복이 없으면 guest 테이블에 매개변수로 들어온 아이디, 비밀번호(SHA256으로 암호화), 이름, 성, 이메일을 insert한다.
	 * @param id 	아이디
	 * @param pwd 	비밀번호
	 * @param fname 이름
	 * @param lname 성
	 * @param email 이메일
	 * @return 		회원가입 성공 여부. 이미 존재하는 ID라면 false를 반환한다. guest테이블에 회원 정보 insert를 성공하면 true를 반환한다.
	 */
	public boolean register(String id, String pwd, String fname, String lname, String email) {
		String sql=String.format("select count(*) from guest where guestID='%s';", id);
		int updatecnt=0;
		int cnt= hoteldbhandle.processSelectCount(sql);
		if(cnt==1) return false;
		
		try {
			sql=String.format("insert into guest values('%s','%s','%s','%s','%s');",id,sha256.encrypt(pwd),fname,lname,email);
			updatecnt=hoteldbhandle.processUpdate(sql);
		}catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		if(updatecnt==1) {
			System.out.println("register complete : your id is : " + id);
		}
		return updatecnt==1;
	}
	/**
	 * @brief 로그아웃한다. 로그인되어있을 경우, guestID를 null로 바꾼다.
	 */
	public void logout() {
		if(guestID==null) {
			System.out.println("you didn't login!");
		}
		else {
			System.out.println("logout!");
			guestID=null;
		}
	}
	/**
	 * @brief 특정 날짜 또는 이후로 예약된 예약의 예약ID, guestID, 체크인 날짜, 체크아웃 날짜, 총 요금, 방 종류, 개수를 보여준다.
	 * @param today 이 날짜 또는 이후로 예약된 예약을 보여준다.
	 */
	public void printReservationList(LocalDate today) {
		String sql = String.format("select * from reservation natural join rooms_type_reserve where guestID='%s' and checkOutDate>='%s'",guestID,today.toString());
		String[] header = {"예약 ID","손님 ID","체크인 날짜","체크아웃 날짜","총 요금","방 종류","개수"};
		hoteldbhandle.processSelectPrint(sql,header);
	}
	
}
