package org.payroll;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class DatabaseManager {

	String ConnectionString;

	Connection conn;
	Statement curs;

	public DatabaseManager(String db) {
		ConnectionString = "jdbc:sqlite:" + db;

		if (!((new File(db)).exists())) {
			connectToDatabase();
			initNewDatabase();

		} else {
			connectToDatabase();
		}
	}

	void connectToDatabase() {
		try {
			conn = DriverManager.getConnection(ConnectionString);
			curs = conn.createStatement();
			curs.setQueryTimeout(30);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
// create table to hold data login id , employee positions , attendance and salary
	void initNewDatabase() {
		try {
			curs.executeUpdate(
					"CREATE TABLE login_ids (id INTEGER NOT NULL PRIMARY KEY, username STRING NOT NULL, password STRING NOT NULL)"
			);
			curs.executeUpdate(
					"INSERT INTO login_ids VALUES(null, \"admin\", \"1\")"
			);
			curs.executeUpdate(
					"CREATE TABLE Position(" +
							"id INTEGER NOT NULL PRIMARY KEY," +
							"pos_name STRING NOT NULL," +
							"hourly_rate INTEGER NOT NULL," +
							"overtime_rate INTEGER NOT NULL)"

			);
			curs.executeUpdate(
					"CREATE TABLE employees(" +
							"id STRING NOT NULL PRIMARY KEY," +
							"first_name STRING NOT NULL," +
							"last_name STRING NOT NULL," +
							"email STRING NOT NULL," +
							"pos_name STRING NOT NULL)"

			);
			curs.executeUpdate(
					"CREATE TABLE Attendance(" +
							"clock_in_id INTEGER NOT NULL PRIMARY KEY," +
							"emp_id STRING NOT NULL," +
							"attendance_date DATE NOT NULL," +
							"clock_in_time TIME NULL," +
							"clock_out_time TIME DEFAULT NULL)"

			);
			curs.executeUpdate(
					"CREATE TABLE emp_salary(" +
							"salary_id INTEGER NOT NULL PRIMARY KEY," +
							"date_salary DATE NOT NULL," +
							"emp_id STRING NOT NULL," +
							"emp_name STRING NOT NULL," +
							"salary_per_day DOUBLE  NOT NULL," +
							"total_time DOUBLE NOT NULL)"
			);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

// verify if the ID exist

	public Boolean verifyLoginId(String id) {
		try {
			return curs.executeQuery(
					"SELECT * FROM login_ids WHERE password=\"" + id + "\""
			).next();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}
//verify if password and username is correct
	public Boolean verifyLoginId(String username, String password) {
		try {
			return curs.executeQuery(
					"SELECT * FROM login_ids WHERE username=\"" + username + "\" AND password=\"" + password + "\""
				).next();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}

	//create new login ID
	public void createLoginId(String username, String password) {
		try {
			curs.executeUpdate("INSERT INTO login_ids VALUES(null, \"" + username + "\", \"" + password + "\")");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	//delete login ID
	public void deleteLoginId(String username) {
		try {
			curs.executeUpdate(
					"DELETE FROM login_ids WHERE username=\"" + username + "\""
				);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}


	public void changePassword(String Password, String newPassword) {
		try {
			curs.executeUpdate(
					"UPDATE login_ids SET password=\"" + newPassword + "\" WHERE password=\"" + Password + "\""
			);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	public void changeUsername(String newusername, String Password) {
		try {
			curs.executeUpdate(
					"UPDATE login_ids SET username=\"" + newusername + "\" WHERE password=\"" + Password + "\""
			);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}


	//check if employee already log in for the day
	public Boolean VerifyClockin(String empID) {
		try {
			return curs.executeQuery(
					"SELECT  clock_in_time FROM Attendance WHERE ( emp_id =\"" + empID + "\")"
							+ "AND ( attendance_date = CURRENT_DATE ) AND ( clock_in_time is not null ) "
			).next();

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return false;
	}

	// check if employee already clock out for the day
	public Boolean VerifyClockOut(String empID) {
		try {
			return curs.executeQuery(
					"SELECT clock_out_time FROM Attendance WHERE ( emp_id =\"" + empID + "\")"
							+ "AND ( attendance_date = CURRENT_DATE ) AND ( clock_out_time is not null ) "
			).next();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}

	//insert today's date into attendance date , current time in clock_in_time
	public void PunchIn(String empID) {
		try {
			curs.executeUpdate(
					"INSERT INTO Attendance ( clock_in_id, emp_id , attendance_date , clock_in_time )" +
							"VALUES( null ,\"" + empID + "\"," + "CURRENT_DATE , CURRENT_TIME )"
			);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

	}

	//update the clock_out_time to current time
	public void PunchOut(String empID) {
		try {
			curs.executeUpdate(
					"UPDATE Attendance " +
							"SET clock_out_time = CURRENT_TIME " +
							"WHERE  emp_id = \"" + empID + "\"" +
							"AND attendance_date = CURRENT_DATE"

			);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

	}

	//Insert salary for employee
	public void InsertEmpSalary(String Date, String Emp_id, String Emp_Fullname, double totalPay, double totalHour) {
		try {
			curs.executeUpdate("INSERT INTO emp_salary ( date_salary , emp_id , emp_name , salary_per_day , total_time )" +
					" VALUES (" + "\"" + Date + "\"," + "\"" + Emp_id + "\"," + "\"" + Emp_Fullname + "\"," + Double.toString(totalPay)
					+ "," + Double.toString(totalHour) + ")");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	// Retrieve the total hours for employee for each day from table , get the hourly rate and OT rate from table positions
	// calculate the total salary and insert into table emp_salary
	public void InsertEmployeeSalaryFromAttandance(String empid) {
		String Date, Emp_id, emp_fullname, positionName;
		double hourly_rate, OT_rate, total_hour, start_minute, end_minute, TotalPay;

		try {
			ResultSet rs = curs.executeQuery(
					"SELECT emp_id,attendance_date " + "FROM Attendance" +
							" WHERE emp_id =\"" + empid + "\" AND attendance_date = CURRENT_DATE"
			);
			while (rs.next()) {
//
				Emp_id = rs.getString("emp_id");
				Date = rs.getString("attendance_date");
				emp_fullname = getEmployeeName(rs.getString("emp_id"));
				positionName = getPosition(Emp_id);
				hourly_rate = getHourlyRate(positionName);
				OT_rate = getOvertimeRate(positionName);
				total_hour = getTotalHours(Emp_id);
				if (total_hour > 8) {
					TotalPay = ((8 * hourly_rate) + ((total_hour - 8) * OT_rate));
				} else {
					TotalPay = (total_hour * hourly_rate);
				}

				InsertEmpSalary(Date, Emp_id, emp_fullname, TotalPay, total_hour);
			}

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	// get the total hours worked from table attendance
	public double getTotalHours(String EmID) {
		ResultSet rs;
		try {
			rs = curs.executeQuery(
					"SELECT strftime('%H',clock_in_time) AS CIHours, strftime('%M',clock_in_time) AS CIMinutes," +
							"strftime('%H',clock_out_time) AS COHours, strftime('%M',clock_out_time) AS COMinutes FROM Attendance " +
							"WHERE emp_id=\"" + EmID + "\" AND attendance_date = CURRENT_DATE"
			);
			if (rs.next()) {
				double CIH = (rs.getInt("CIHours") * 60); // get clockInTime Hours convert to minutes
				double COH = (rs.getInt("COHours") * 60); // get clockOutTime Hours convert to minutes
				double CIM = rs.getDouble("CIMinutes"); // get ClockInTime minutes
				double COM = rs.getDouble("COMinutes");// get ClockOutTime minutes
				double difference = ((COM + COH) - (CIM + CIH)); // calculate the difference between clock in and clock out in minutes
				double DiffHours = (difference / 60); // convert from minutes to hours
				return DiffHours;
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return 0;
	}

	//check if position exist
	public Boolean existsPosition(String pos_name) {
		try {
			return curs.executeQuery(
					"SELECT * FROM Position WHERE pos_name=\"" + pos_name + "\""
			).next();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}

	public void newPosition(String pos_name, int hourly_rate, int overtime_rate) {

		try {
			curs.executeUpdate(
					"INSERT INTO Position VALUES(" +
							"null," +
							"\"" + pos_name + "\" ," +
							Integer.toString(hourly_rate) + "," +
							Integer.toString(overtime_rate) +
							")"
			);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}





	//delete position and employee with that position
	public void deletePosition(String pos_name) {
		try {
			curs.executeUpdate(
					"DELETE FROM Position WHERE pos_name=\"" + pos_name + "\""
			);

			ResultSet rs = curs.executeQuery(
					"SELECT id FROM employees WHERE pos_name =\"" + pos_name + "\""
			);

			deleteEmployee(rs.getString("id"));

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	// saved and return array for all the positions name
	public ArrayList<String> getListOfPositionName() {
		ArrayList<String> Positions = new ArrayList<String>();

		try {
			ResultSet rs = curs.executeQuery("SELECT pos_name FROM Position");

			while (rs.next()) {
				Positions.add(rs.getString("pos_name"));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return Positions;
	}

	// Retrieve all the data from position table and store it into Object position , then return array of object position
	// to be used in Jtable in Jframe
	public Object[][] getPositions() {
		ArrayList<Object[]> Positions = new ArrayList<Object[]>();

		try {
			ResultSet rs = curs.executeQuery("SELECT * FROM Position");

			while (rs.next()) {

				Object[] temp = {
						rs.getInt("id"),
						rs.getString("pos_name"),
						rs.getInt("hourly_rate"),
						rs.getInt("overtime_rate")


				};

				Positions.add(temp);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return Positions.toArray(new Object[Positions.size()][]);
	}

	//Insert new employee data into table employee
	public void createEmployee(String Empid, String fn, String ln, String email, String position) {
		try {
			curs.executeUpdate("INSERT INTO employees VALUES(" +
					"\"" + Empid + "\"," +
					"\"" + fn + "\"," +
					"\"" + ln + "\"," +
					"\"" + email + "\"," +
					"\"" + position + "\"" +
					")");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	//verify if employee ID exist
	public Boolean existsEmployeeID(String Empid) {
		try {
			return curs.executeQuery(
					"SELECT * FROM employees WHERE id=\"" + Empid + "\""
			).next();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}


	//store all employee ID into array and return it
	public ArrayList<String> getEmployeeID() {
		ArrayList<String> EmployeeID = new ArrayList<String>();

		try {
			ResultSet rs = curs.executeQuery("SELECT id FROM employees");

			while (rs.next()) {
				EmployeeID.add(rs.getString("id"));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return EmployeeID;
	}

	//delete employee data that match the input employee id as well as employee data in table attendance and
	//salary with same ID
	public void deleteEmployee(String Empid) {
		try {
			curs.executeUpdate(
					"DELETE FROM employees WHERE id=\"" + Empid + "\""
			);
			curs.executeUpdate(
					"DELETE FROM Attendance WHERE Emp_id=\"" + Empid + "\""
			);
			curs.executeUpdate(
					"DELETE FROM emp_salary WHERE Emp_id=\"" + Empid + "\""
			);

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	//update employee information
	public void updateEmployee(String Empid, String fn, String ln, String email, String position) {
		try {
			curs.executeUpdate(
					"UPDATE employees SET " +
							"first_name=\"" + fn + "\"," +
							"last_name=\"" + ln + "\"," +
							"email=\"" + email + "\"," +
							"pos_name=\"" + position + "\" " +
							"WHERE id=\"" + Empid + "\""
			);
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	// get and return full name of employee from employee ID
	public String getEmployeeName(String empid) {
		String FullName;
		try {
			ResultSet rs = curs.executeQuery("SELECT  first_name , last_name  FROM employees WHERE id=\""
					+ empid + "\"");

			if (rs.next()) {
				return (FullName = (rs.getString("first_name") + " " + rs.getString("last_name")));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return "";
	}

	//get all employee data and store into employee object . Store and return employee Object in an array of object
	//to be used in Jtable for Jframe
	public Object[][] getEmployees() {

		ArrayList<Object[]> employees = new ArrayList<Object[]>();
		ResultSet rs;

		try {
			rs = curs.executeQuery(
					"SELECT * FROM employees"
			);

			while (rs.next()) {
				Object[] temp = {
						rs.getString("id"),
						rs.getString("first_name"),
						rs.getString("last_name"),
						rs.getString("email"),
						rs.getString("pos_name")
				};

				employees.add(temp);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return employees.toArray(new Object[employees.size()][]);
	}

	// get the employee position from employee ID
	public String getPosition(String EmpID) {
		try {
			ResultSet rs = curs.executeQuery("SELECT pos_name FROM employees WHERE id =\"" + EmpID + "\"");

			if (rs.next())
				return rs.getString("pos_name");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return "";
	}

	//get the hourly rate from position with matching position name
	public int getHourlyRate(String pos_name) {
		try {
			ResultSet rs = curs.executeQuery("SELECT hourly_rate FROM Position WHERE pos_name =\"" + pos_name + "\"");

			if (rs.next())
				return rs.getInt("hourly_rate");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return 0;
	}

	//get the hourly rate from position with matching position name
	public int getOvertimeRate(String pos_name) {
		try {
			ResultSet rs = curs.executeQuery("SELECT overtime_rate FROM Position WHERE pos_name =\"" + pos_name + "\"");

			if (rs.next())
				return rs.getInt("overtime_rate");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return 0;
	}

//get employee attendance data on a particular date from table attendance . Store data in attendance Object and
	//store the object in an array of object and return it to be used in Jtable in the Jframe
	public Object[][] getAttendance(String date) {
		ArrayList<Object[]> Attendance = new ArrayList<Object[]>();
		ResultSet rs;
		int id;
		String empID, emPName, CIT, COT, dt;


		try {
			rs = curs.executeQuery("SELECT clock_in_id,emp_id,strftime('%H : %M : %S',clock_in_time) AS clockIn," +
					"strftime('%H : %M : %S',clock_out_time) AS clockOut, attendance_date AS dates " +
					"FROM Attendance WHERE clock_out_time IS NOT NULL");

			while (rs.next()) {
				id = rs.getInt("clock_in_id");
				empID = rs.getString("emp_id");
				CIT = rs.getString("clockIn"); // get clock_in_time as String
				COT = rs.getString("clockOut"); //get clock_out_time as String
				dt = rs.getString("dates");
				emPName = getEmployeeName(empID);

				if ( dt.equals(date) ) // if the two date is equal then add the data for that row into array
				{
					Object[] temp = {id, empID, emPName, dt, CIT, COT}; //Object to hold employee Attendance data
					Attendance.add(temp); // add object to array
				}
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return Attendance.toArray(new Object[Attendance.size()][]);

	}

	//get All employee attendance data from table attendance . Store data in attendance Object and
	//store the object in an array of object and return it to be used in Jtable in the Jframe
	public Object[][] getAllAttendance() {
		ArrayList<Object[]> Attendance = new ArrayList<Object[]>();
		ResultSet rs;
		int id;
		String empID, emPName, dt, CIT, COT;


		try {
			rs = curs.executeQuery("SELECT clock_in_id,emp_id,strftime('%H:%M:%S',clock_in_time) AS clockIn," +
					"strftime('%H:%M:%S',clock_out_time) AS clockOut,attendance_date AS dates FROM Attendance" +
					" WHERE clock_out_time IS NOT NULL ");

			while (rs.next()) {
				id = rs.getInt("clock_in_id");
				empID = rs.getString("emp_id");
				CIT = rs.getString("clockIn");
				COT = rs.getString("clockOut");
				dt = rs.getString("dates");

				emPName = getEmployeeName(empID);

				Object[] temp = {id, empID, emPName, dt, CIT, COT};

				Attendance.add(temp);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return Attendance.toArray(new Object[Attendance.size()][]);

	}

	// total up the hours and pay for each employee and group them by emp_id , month and year
	// return array Object salary to be used in Jtable
	public Object[][] getAllMonthlySalary() {
		ArrayList<Object[]> Salary = new ArrayList<Object[]>();
		String EmID, EmFN;
		int m, y;
		double TP, TH;

		try {
			ResultSet rs = curs.executeQuery
					(
							"SELECT  emp_id, emp_name, SUM(salary_per_day) AS Tot_sal ,SUM(total_time) AS Tot_time,strftime('%Y',date_salary) AS sal_year," +
									"strftime('%m',date_salary) AS sal_month FROM emp_salary " +
									"GROUP BY emp_id , sal_year ,sal_month " +
									"ORDER BY sal_month, sal_year DESC  "
					);

			while (rs.next()) {


				double total_salary_2dp = Math.round(rs.getDouble("Tot_sal")*100.0)/100.0;
				double total_time_2dp = Math.round(rs.getDouble("Tot_time")*100.0)/100.0;

				Object[] temp = {
						rs.getString("emp_id"),
						rs.getString("emp_name"),
						rs.getInt("sal_month"),
						rs.getInt("sal_year"),
						total_salary_2dp,
						total_time_2dp
				};

				Salary.add(temp);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return Salary.toArray(new Object[Salary.size()][]);

	}

	// total up the hours and pay for each employee and group them by emp_id , month and year
	// return array Object salary to be used in Jtable , This one can be is according to the month and year chosen by user
	public Object[][] getSalaryByMonthAndYear(int month, int year) {
		ArrayList<Object[]> Salary = new ArrayList<Object[]>();
		String EmID, EmFN;
		int m, y;
		double TP, TH;


		try {

			ResultSet rs = curs.executeQuery
					(
							"SELECT  emp_id, emp_name, SUM(salary_per_day) AS tot_salary ,SUM(total_time) AS tot_time , strftime('%Y',date_salary) AS sal_year, " +
									"strftime('%m',date_salary) AS sal_month  FROM emp_salary " +
									"GROUP BY  emp_id , sal_year ,sal_month"

					);

			while (rs.next()) {

				double total_salary_2dp = Math.round(rs.getDouble("tot_salary") * 100.0)/100.0;
				double total_time_2dp = Math.round(rs.getDouble("Tot_time")*100.0)/100.0;

				Object[] temp = {
						rs.getString("emp_id"),
						rs.getString("emp_name"),
						rs.getInt("sal_month"),
						rs.getInt("sal_year"),
						total_salary_2dp,
						total_time_2dp

				};

				m = rs.getInt("sal_month");
				y = rs.getInt("sal_year");

				if ((m == month) & (y == year)) // if the column year and month in table match with user year and month input , add object to array
				{
					Salary.add(temp);
				}

			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return Salary.toArray(new Object[Salary.size()][]);

	}

}
