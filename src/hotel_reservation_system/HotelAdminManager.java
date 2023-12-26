package hotel_reservation_system;

import java.security.NoSuchAlgorithmException;

import db_handler.HotelDBHandler;
import encryption.SHA256;
/**
 * @brief manager로 로그인 및 로그아웃한다.
 */
public class HotelAdminManager {
	private HotelDBHandler hoteldbhandle;
	private SHA256 sha256=null;
	private String managerID=null;
	
	/**
	 * @brief 	현재 로그인된 manager의 ID를 반환한다.
	 * @return 	현재 로그인된 manager의 ID
	 */
	public String getManagerID() {
		return managerID;
	}
	
	public HotelAdminManager(HotelDBHandler h1) {
		hoteldbhandle=h1;
		sha256=new SHA256();
	}
	
	/**
	 * @brief 	입력받은 id와 password로 로그인한다. manager 데이터베이스에 일치하는 id와 password가 있는지 확인한다.
	 * @param 	id 아이디
	 * @param 	pwd 비밀번호
	 * @return 	로그인 성공 여부. true는 로그인 성공, false는 로그인 실패이다.
	 */
	public boolean login(String id, String pwd) {
		if(managerID!=null) {
			System.out.println("already login!");
			return false;
		}
		int cnt=0;
		try {
			
			String sql=String.format("select count(*) from manager where managerID='%s' and pwd='%s';", id,sha256.encrypt(pwd));
			cnt=hoteldbhandle.processSelectCount(sql);
			if(cnt==1) managerID=id;
		}catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return cnt==1;
			
	}
	/**
	 * @brief manager에서 로그아웃한다.
	 */
	public void logout() {
		if(managerID==null) {
			System.out.println("alredy logout!");
			return;
		}
		managerID=null;
	}
}
