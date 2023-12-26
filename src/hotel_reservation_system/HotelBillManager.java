package hotel_reservation_system;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import db_handler.HotelDBHandler;

import java.math.BigDecimal;

/**
 * @brief 결제 수행, 결제 취소, 결제 내역을 확인할 수 있는 클래스이다.
 */
public class HotelBillManager {
	private HotelDBHandler hoteldbhandle;
	
	public HotelBillManager(HotelDBHandler h1) {
		hoteldbhandle=h1;
	}
	/**
	 * @brief 				입력받은 정보로 결제 정보를 bill 데이터베이스에 입력한다.
	 * @detail 				먼저 새로운 billID를 얻는다. 이때 bill과 bill_cancel 데이터베이스에 있는 billID값을 모두 고려한다.
	 * 						그리고 결제 정보를 insert한다. (billID,reservationID,guestID,CURRENT_TIMESTAMP(현재 시각),amount,paymentmethod)
	 * @param reservationID 결제할 예약 ID
	 * @param guestID 		손님ID
	 * @param amount 		결제 금액
	 * @param paymentmethod 결제 방법
	 * @return 				테이블에 insert 성공하면 true, 실패하면 false 반환
	 */
	public boolean insertBillInfo(String reservationID, String guestID, int amount, String paymentmethod) {
		BigDecimal newid1=hoteldbhandle.getNewID("bill","billID");
		BigDecimal newid2=hoteldbhandle.getNewID("bill_cancel", "billID");
		
		BigDecimal newid;
		if(newid1.compareTo(newid2)>=0) {
			newid=newid1;
		}
		else {
			newid=newid2;
		}
		String sql=String.format("insert into bill values(%s,%s,'%s',CURRENT_TIMESTAMP,%d,'%s');", newid.toString(),reservationID,guestID,amount,paymentmethod);
		int updaterow=hoteldbhandle.processUpdate(sql);
		if(updaterow==1) {
			System.out.println(Integer.toString(amount) + "원 결제되었습니다.");
			return true;
		}
		return false;
	}
	/**
	 * @brief 	매개변수로 받은 ID를 가진 guest가 매개변수로 받은 예약ID로 예약된 예약에 해당하는 결제를 취소한다.
	 * @detail 	bill 테이블에 reservationID attribute가 입력받은 예약 ID인 것을 삭제하고, bill_cancel에 취소된 결제 정보를 넣는다.
	 * @param 	reservationID 예약 ID
	 * @param 	guestID 이 결제를 취소하려는 guest의 ID. 이 guestID에 해당하는 guest가  reservationID에 해당하는 예약을 결제했을 경우에만 취소된다.
	 * @return 	결제취소 성공여부 true는 결제취소 성공, false는 결제취소 실패이다.
	 */
	public boolean cancelBill(String reservationID, String guestID) {
		String sql=String.format("select * from bill where reservationID=%s and guestID='%s';", reservationID,guestID);
		ResultSet rs=hoteldbhandle.processSelect(sql);
		try {
			if(rs.next()) {
				int amount=rs.getInt("amount");
				String paymentmethod=rs.getString("payment_method");
				String billID = rs.getString("billID");
				rs.close();
				
				hoteldbhandle.processUpdate(String.format("delete from bill where reservationID=%s;", reservationID));
				hoteldbhandle.processUpdate(String.format("insert into bill_cancel values(%s,%s,'%s',%d,'%s',CURRENT_TIMESTAMP);", billID,reservationID,guestID,amount,paymentmethod));
				System.out.println(String.format("%d원 결제가 취소되었습니다.", amount));
				return true;
			}
			else {
				System.out.println("reservationID and guestID no match!");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * @brief start날짜 이후 end날짜 이전에 결제된 내역을 보여준다.
	 * @param start 시작 날짜 이 날짜를 포함하여 이후에 결제된 내역을 보여준다.
	 * @param end 끝 날짜 이 날짜를 포함하여 이전에 결제된 내역을 보여준다.
	 */
	public void billList(LocalDate start, LocalDate end) {
		String[] header = {"billID","예약ID","손님ID","결제날짜","결제금액","결제방법"};
		hoteldbhandle.processSelectPrint(String.format("select * from bill where billDate>='%s' and billDate <='%s';", start.toString(),end.toString()), header);
	}
}
