package db_handler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.io.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @brief hotel database에 연결하고, 입력받은 SQL문을 실행한다.
 */
public class HotelDBHandler {
	private final String JDBC_DRIVER = "org.postgresql.Driver";
	private final String DB_URL = "jdbc:postgresql://localhost:5432/hoteldb";
	
	private final String USER = "dbdb2023";
	private final String PASS = "dbdb!2023";
	
	private Connection conn = null;
	private DatabaseMetaData mtdata = null;
	private Statement stmt = null;
	private ResultSetMetaData rsmeta = null;
	
	/**
	 * @brief hotel 데이터베이스에 접속한다. processSelect, processUpdate 메소드를 호출하기 전에 이 함수를 먼저 호출해야한다.
	 * @return none
	 */
	public void connect() {
		
		try {
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL,USER,PASS);
			stmt = conn.createStatement();
		}catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
	}
	
	/**
	 * @brief hotel 데이터베이스와 연결을 해제한다.
	 * @return none
	 */
	public void disconnect() {
		try{
            if(conn != null) conn.close();
        } catch (Exception e){
        	e.printStackTrace();
        }
	}
	/**
	 * @brief 입력받은 SQL(update, insert, delete)를 실행하고 업데이트된 행 개수를 반환한다.
	 * @param strSQL 실행할 SQL문(update, insert, delete)
	 * @return 업데이트된 행 개수
	 */
	public int processUpdate(String strSQL) {
		int count=0;
		try {
			stmt.execute(strSQL);
			count = stmt.getUpdateCount();
	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return count;
	}
	/**
	 * @brief 입력받은 SQL(select count(*)...)를 실행하고 count 값을 반환한다.
	 * @param strSQL 실행할 SQL문(select count(*))
	 * @return count 값
	 */
	public int processSelectCount(String strSQL) {
		int cnt=0;
		try {
			stmt.execute(strSQL);
			ResultSet rs = stmt.getResultSet();
			while(rs.next()) {
				cnt=rs.getInt(1);
			}
			rs.close();
		}
		catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return cnt;
	}
	/**
	 * @brief 입력받은 SQL(select)문을 실행하고 결과 행들을 출력한다.
	 * @param strSQL 실행할 SQL(select)문
	 * @param header 결과 행들을 표시할 때 맨 상단에 표시할 attribute 이름
	 */
	public void processSelectPrint(String strSQL, String[] header) {
		ResultSet rs=this.processSelect(strSQL);
		printResultSet(rs,header);
		try {
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @brief 결과 행들을 출력한다.
	 * @param rs sql문을 실행한 결과가 들어있는 resultset
	 * @param header 결과 행들을 출력할 때 맨 상단에 표시할 attribute
	 */
	public void printResultSet(ResultSet rs,String[] header)
	{
		try {
			rsmeta = rs.getMetaData();
			print_dashes(rsmeta);
			print_headers(rsmeta,header);
			print_dashes(rsmeta);
			print_datarows(rsmeta, rs);
			print_dashes(rsmeta);
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		
	}
	/**
	 * @brief 테이블(tbl)에 행을 넣을 때 필요한 고유 id값(idname attribute)를 생성한다.
	 * @detail id값은 행을 넣을 때 시각(20231225등)에 고유한 다섯자리 숫자를 붙어 생성한다. 오늘날짜가 2023년 12월25일인 경우, 해당 table에
	 * 			idname attribute가 2023122500000이상인 것 중 가장 큰 값을 찾아 그 값에 1을 더하여 새로운 id값을 생성한다.
	 * @param tbl 테이블 이름
	 * @param idname 고유한 id가 저장되는 attribute 이름
	 * @return 새로운 ID값
	 */
	public BigDecimal getNewID(String tbl,String idname) {
		LocalDate date=LocalDate.now();
		BigDecimal newid=new BigDecimal(String.format("%tY%tm%td00000", date,date,date));
		String sql=String.format("select %s from %s where %s >= %tY%tm%td00000 order by %s desc;", idname,tbl,idname,date,date,date,idname);
		try {
			ResultSet rs = processSelect(sql);
			if(rs.next()) {
				newid=rs.getBigDecimal(idname);
				newid=newid.add(new BigDecimal("1"));
			}
			rs.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newid;
	}
	/**
	 * @brief sql(select)문을 실행하고 실행한 결과를 resultset으로 반환한다.
	 * @param strSQL 실행할 sql문(select ...)
	 * @return sql문을 실행한 결과로 받은 resultset
	 */
	public ResultSet processSelect(String strSQL) {
		try {
			stmt.execute(strSQL);
			ResultSet rs = stmt.getResultSet();
			return rs;
		}
		catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * @brief 출력할 데이터에 맞게 구분선을 출력한다.
	 * @param rsmeta resultSetMetaData
	 * @throws SQLException
	 */
	private void print_dashes(ResultSetMetaData rsmeta) throws SQLException
	{
		int i,j;
		int colsize;
		int numberOfColumns = rsmeta.getColumnCount();

		System.out.print("+");

		for (i =1;i <= numberOfColumns;i++){
			colsize =rsmeta.getColumnDisplaySize(i);
			for (j =0;j <colsize +2;j++)
					System.out.print('-');
			System.out.print('+');
		}
		System.out.println();
	}
	/**
	 * @brief attribute이름을 출력한다.
	 * @param rsmeta ResultSetMetaData
	 * @param header 출력할 attribute 이름
	 * @throws SQLException
	 */
	private void print_headers(ResultSetMetaData rsmeta, String[] header) throws SQLException
	{
		int i,j;
		int colsize;
		String colName = null;
		int numberOfColumns = rsmeta.getColumnCount();
		if(numberOfColumns != header.length) {
			System.out.println("number of Column and header length is not equal!");
			return;
		}
		
		System.out.print("|");
		for (i =1;i <= numberOfColumns;i++){
			colsize =rsmeta.getColumnDisplaySize(i);
			colName =header[i-1];
			System.out.print(colName);
			// If data is maller than column, we will add some spaces
			for(j=1;j<=colsize+2- colName.length();j++)
				System.out.print(" ");
			System.out.print("|");

			//System.out.print('+');
		}
		System.out.println();
	}
	/**
	 * @brief sql문을 실행하여 받은 데이터를 출력한다.
	 * @param rsmeta ResultSetMetaData
	 * @param rs ResultSet
	 * @throws SQLException
	 */
	private void print_datarows (ResultSetMetaData rsmeta, ResultSet rs) throws SQLException
	{
		int i,j;
		int colsize;
		int numberOfColumns = rsmeta.getColumnCount();
		String strData = null;
		if(rs.isAfterLast()) {
			System.out.println("No search result");
			return;
		}
		
		while(rs.next()){  // Move the cursor

			System.out.print("|");
			for (i =1;i <= numberOfColumns;i++)
			{
				colsize =rsmeta.getColumnDisplaySize(i);
				strData = rs.getString(i);
				if (rs.wasNull())  // NULL value check
				{
					strData = "NULL";
				}
				if(strData.length()> colsize+2) // Data is larger than the column size
					System.out.print(strData.substring(0, colsize+2));
				else
					System.out.print(strData);
				// If data is maller than column, we will add some spaces
				for(j=1;j<=colsize+2- strData.length();j++)
					System.out.print(" ");
				System.out.print("|");
			}
			System.out.println();
		}
	}
}
