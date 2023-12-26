package hotel_reservation_system;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import db_handler.HotelDBHandler;
/**
 * @brief housekeeping을 관리하는 클래스이다.
 */
public class HotelHouseKeepManager {
	private HotelDBHandler hoteldbhandle;
	
	
	public HotelHouseKeepManager(HotelDBHandler h1) {
		hoteldbhandle=h1;
		
		
	}
	/**
	 * @brief room_num에 해당하는 방에 checkIn날짜 하루전부터 checkOut날짜까지 하우스키퍼내역을 할당한다.
	 * @detail room_num에 해당하는 방에 checkIn날짜 하루전부터 checkOut날짜까지 하우스키퍼내역을 할당한다.
	 * 			이때 housekeeper 할당은 housekeep 할당내역이 가장 적은 housekeeper부터 이루어진다.
	 * 			예를 들어 A,B,C 세 사람이 있고, 각각 housekeep를 각각 총 3,4,5번 하였다면, 먼저 checkIn날짜 housekeep은 A에게 할당된다.
	 * 			이제 housekeep내역이 4,4,5이므로, 그 다음 checkIn+1 날짜 housekeep도 A에게 할당된다.
	 * 			이제 housekeep내역이 5,4,5이므로, checkIn+2 날짜 housekeep은 B에게 할당된다.
	 * @param room_num 방 번호
	 * @param checkIn 체크인날짜
	 * @param checkOut 체크아웃 날짜
	 */
	public void insertHouseKeep(int room_num, LocalDate checkIn,LocalDate checkOut) {
		
		List<String> housekeeperList = new ArrayList<>();
		ResultSet r = hoteldbhandle.processSelect("select housekeeperID from housekeeper;");
		try {
			while(r.next()) {
				housekeeperList.add(r.getString(1));
			}
			r.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LocalDate start = LocalDate.of(checkIn.getYear(), checkIn.getMonth(), checkIn.getDayOfMonth()).minusDays(1);
		while(start.compareTo(checkOut)<=0) {
			int cnt = hoteldbhandle.processSelectCount(String.format("select count(*) from housekeep_assignment where room_num=%d and assign_date='%s';", room_num,start.toString()));
			
			if(cnt==0) {
				String housekeeperID =housekeeperList.get(0);
				int min = hoteldbhandle.processSelectCount(String.format("select count(*) from housekeep_assignment where housekeeperID='%s';", housekeeperID));
				for(String housekeeper:housekeeperList) {
					int tmp = hoteldbhandle.processSelectCount(String.format("select count(*) from housekeep_assignment where housekeeperID='%s';", housekeeper));
					if(tmp<min) {
						min=tmp;
						housekeeperID = housekeeper;
					}
				}

				hoteldbhandle.processUpdate(String.format("insert into housekeep_assignment values(%d,'%s','%s','%s');", room_num,housekeeperID,start.toString(),"incomplete"));
				
			}
			
			start=start.plusDays(1);
		}
	}
	/**
	 * @brief 			특정 방 번호에 해당하는 방에 체크인 날짜 하루전부터 체크 아웃날짜까지 하우스키퍼내역을 삭제한다.
	 * @detail 			특정 방 번호에 해당하는 방에 체크인 날짜 하루전부터 체크 아웃날짜까지 하우스키퍼내역을 삭제한다.
	 * 					단,checkIn날짜 하루 전부터 checkOut날짜 하루 후까지 범위에 room_num에 해당하는 방에 예약이 있을 경우 해당 날짜의 housekeep내역은 삭제하지 않는다.
	 * 					예를 들어, 체크인 날짜가 12월24일, 체크아웃 날짜가 12월25일인 방 번호 101의 예약이 취소되어 deleteHouseKeep이 호출되었다고 하자.
	 * 					만약 방 번호 101에 체크인 날짜가 12월 23일, 체크아웃 날짜가 12월24일인 예약이 있으면 12월23일, 12월24일 housekeep내역은 삭제하지 않는다.
	 * 					만약 방 번호 101에 체크인 날짜가 12월 25일, 체크아웃 날짜가 12월27일인 예약이 있으면 12월24일, 12월25일 housekeep내역은 삭제하지 않는다.
	 * @param room_num 	방 번호. 이 방에 housekeep을 삭제한다.
	 * @param checkIn 	체크인 날짜 이 날짜 하루 전부터 housekeep 내역을 삭제한다.
	 * @param checkOut 	체크아웃 날짜. 이 날짜까지 housekeep 내역을 삭제한다.
	 */
	public void deleteHouseKeep(int room_num,LocalDate checkIn, LocalDate checkOut) {
		LocalDate start = LocalDate.of(checkIn.getYear(), checkIn.getMonth(), checkIn.getDayOfMonth()).minusDays(1);
		while(start.compareTo(checkOut)<=0) {
			int cnt = hoteldbhandle.processSelectCount(String.format("select count(*) from rooms_reserve where room_num=%d and (reserve_date='%s' or reserve_date='%s');", room_num,start.toString(),start.plusDays(1).toString()));
			
			if(cnt==0) {
				hoteldbhandle.processUpdate(String.format("delete from housekeep_assignment where room_num=%d and assign_date='%s';", room_num,start.toString()));
			}
			
			start=start.plusDays(1);
		}
	}
	/**
	 * @brief 		특정 날짜 또는 그 이후에 할당된 housekeep내역을 보여준다.
	 * @param today 이 날짜 또는 그 이후로 할당된 housekeep 내역을 보여준다.
	 */
	public void showHouseKeep(LocalDate today) {
		String[] header= {"방 번호","하우스키퍼 ID","날짜","상태"};
		hoteldbhandle.processSelectPrint(String.format("select * from housekeep_assignment where assign_date >= '%s' order by assign_date;", today.toString()),header);
	}
	/**
	 * @brief 			특정 방 번호에 해당하는 방에 특정 날짜에 할당된 housekeep내역의 status를 바꾼다.
	 * @param room_num 	방 번호. 이 방에 할당된 housekeep 내역의 status를 바꾼다.
	 * @param today 	이 날짜에 할당된 housekeep내역의 status를 바꾼다
	 * @param status 	바꿀 status. status는 'complete' 또는 'incomplete'이다.
	 */
	public void changeHouseKeep(int room_num,LocalDate today, String status) {
		if(hoteldbhandle.processUpdate(String.format("update housekeep_assignment set status='%s' where room_num=%d and assign_date = '%s';", status,room_num,today.toString()))>=1) {
			System.out.println("update complete");
		}
		else {
			System.out.println("update failed");
		}
	}
}
