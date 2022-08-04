package project;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DAO {

    public final Connection conn;

    public DAO(String dbName, String user, String password) throws SQLException {
        String url = "jdbc:mysql://localhost/" + dbName;
        conn = DriverManager.getConnection(url, user, password);
    }

    public User getUserOnEmail(String email) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Users WHERE Email=?");
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();
        User user = null;
        if(rs.next()) {
            int uid = rs.getInt("UID");
            String sin = rs.getString("SIN");
            String occupation = rs.getString("Occupation");
            String password = rs.getString("Password");
            String dob = rs.getString("DOB");
            String name = rs.getString("Name");
            int aid = rs.getInt("AID");
            user = new User(uid, sin, email, occupation, password, dob, name, aid);
        }
        return user;
    }
    public Renter getRenterFromUser(User user) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Renters where UID=?");
        stmt.setInt(1, user.getUid());
        ResultSet rs = stmt.executeQuery();
        Renter renter = null;
        if(rs.next()) {
            String creditCard = rs.getString("CreditCard");
            renter = new Renter(user, creditCard);
        }
        return renter;
    }
    public Host getHostFromUser(User user) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Hosts where UID=?");
        stmt.setInt(1, user.getUid());
        ResultSet rs = stmt.executeQuery();
        Host host = null;
        if(rs.next()) {
            host = new Host(user);
        }
        return host;
    }

    public int getAddressID(String address, String city, String country, String postalCode) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Addresses WHERE Address=? AND City=? AND Country=? AND PostalCode=?");
        stmt.setString(1, address);
        stmt.setString(2, city);
        stmt.setString(3, country);
        stmt.setString(4, postalCode);
        ResultSet rs = stmt.executeQuery();
        int result = -1;
        if(rs.next()) {
            result = rs.getInt("AID");
        }
        return result;
    }

    public int getListingID(int hid, String type, double latitude, double longitude, int aid, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Listings WHERE uid=? AND type=? AND latitude=? AND longitude=? AND aid=? AND status=?");
        stmt.setInt(1, hid);
        stmt.setString(2, type);
        stmt.setDouble(3, latitude);
        stmt.setDouble(4, longitude);
        stmt.setInt(5, aid);
        stmt.setString(6, status);
        ResultSet rs = stmt.executeQuery();
        int result = -1;
        if (rs.next()) {
            result = rs.getInt("LID");
        }
        return result;
    }

    public ArrayList<String> getAmenitiesListByCategory(String category) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Amenities WHERE category=?");
        stmt.setString(1, category);
        ResultSet rs = stmt.executeQuery();
        ArrayList<String> amenities = new ArrayList<>();
        while (rs.next()) {
            amenities.add(rs.getString("Description"));
        }

        return amenities;
    }

    /* Returns true if there are already availabilities in the given date range. */
    public boolean checkAvailabilitiesInRange(int lid, String start, String end) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Calendars WHERE lid=? AND Day BETWEEN ? AND ?");
        stmt.setInt(1, lid);
        stmt.setString(2, start);
        stmt.setString(3, end);
        ResultSet rs = stmt.executeQuery();
        return rs.next();
    }

    public void createAvailabilitiesInRange(int lid, String start, String end, double price) throws SQLException {
        LocalDate curDate = LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endDate = LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE);

        endDate = endDate.plusDays(1); // add 1 day to include range's endpoints

        while (!curDate.equals(endDate)) {
            if (!checkAvailabilitiesInRange(lid, curDate.toString(), curDate.toString())) {
                // no availability on this day, so create one
                createAvailability(lid, curDate.toString(), price, "AVAILABLE");
            }
            curDate = curDate.plusDays(1);
        }
    }

    public void createAvailability(int lid, String day, double price, String status) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Calendars VALUES (?, ?, ?, ?)");
        stmt.setInt(1, lid);
        stmt.setString(2, day);
        stmt.setDouble(3, price);
        stmt.setString(4, status);
        stmt.executeUpdate();
    }

    public int createAddress(String address, String city, String country, String postalCode) throws SQLException {
        int aid = getAddressID(address, city, country, postalCode);
        if ( aid != -1) {
            return aid;
        }
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Addresses(Address, City, Country, PostalCode) VALUES (?, ?, ?, ?)");
        stmt.setString(1, address);
        stmt.setString(2, city);
        stmt.setString(3, country);
        stmt.setString(4, postalCode);
        stmt.executeUpdate();
        return getAddressID(address, city, country, postalCode);
    }

    public int createUser(String sin, String name, String dob, String occupation, String email,
                              String password, int aid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Users(SIN, Name, DOB, Occupation, Email, Password, AID, Status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        stmt.setString(1, sin);
        stmt.setString(2, name);
        stmt.setString(3, dob);
        stmt.setString(4, occupation);
        stmt.setString(5, email);
        stmt.setString(6, password);
        stmt.setInt(7, aid);
        stmt.setString(8, "ACTIVE");
        stmt.executeUpdate();
        return getUserOnEmail(email).getUid();
    }

    public void createRenter(int uid, String creditCard) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Renters VALUES (?, ?)");
        stmt.setInt(1, uid);
        stmt.setString(2, creditCard);
        stmt.executeUpdate();
    }

    public void createHost(int uid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Hosts VALUES (?)");
        stmt.setInt(1, uid);
        stmt.executeUpdate();
    }

    public void createView(String view, String query) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("CREATE VIEW "+view+" AS ("+query+")");
        stmt.execute();
    }

    public void deleteView(String view) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DROP VIEW IF EXISTS "+view);
        stmt.execute();
    }

    public ArrayList<Listing> getListingsFromFilter3(String str) throws SQLException {
        PreparedStatement stmt;

        if (str.equals("ASC") || str.equals("DESC")) {
            stmt = conn.prepareStatement("SELECT L.*, AVG(Price) as Price FROM Filter3 L, Calendars C " +
                    "WHERE L.LID=C.LID GROUP BY L.LID ORDER BY Price " + str);
        } else {
            stmt = conn.prepareStatement("SELECT * FROM Filter3");
        }

        ResultSet rs = stmt.executeQuery();
        ArrayList<Listing> result = new ArrayList<>();
        while(rs.next()) {
            int lid = rs.getInt("LID");
            String type = rs.getString("Type");
            double latitude = rs.getDouble("Latitude");
            double longitude = rs.getDouble("Longitude");

            int aid = rs.getInt("AID");
            String address = rs.getString("Address");
            String city = rs.getString("City");
            String country = rs.getString("Country");
            String postalCode = rs.getString("PostalCode");

            Address newAddress = new Address(aid, address, city, country, postalCode);
            result.add(new Listing(lid, type, latitude, longitude, newAddress));
        }
        return result;
    }

    public ArrayList<Listing> getListingsFromHost(int hid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Listings NATURAL JOIN Addresses WHERE uid=? AND status='ACTIVE'");
        stmt.setInt(1, hid);
        
        ResultSet rs = stmt.executeQuery();
        ArrayList<Listing> result = new ArrayList<>();
        while(rs.next()) {
            int lid = rs.getInt("LID");
            String type = rs.getString("Type");
            double latitude = rs.getDouble("Latitude");
            double longitude = rs.getDouble("Longitude");

            int aid = rs.getInt("AID");
            String address = rs.getString("Address");
            String city = rs.getString("City");
            String country = rs.getString("Country");
            String postalCode = rs.getString("PostalCode");

            Address newAddress = new Address(aid, address, city, country, postalCode);
            result.add(new Listing(lid, type, latitude, longitude, newAddress));
        }
        return result;
    }

    public int createListing(int hid, String type, double latitude, double longitude, int aid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Listings(UID, Type, Latitude, Longitude, AID, Status) " +
                        "VALUES (?, ?, ?, ?, ?, ?)");
        stmt.setInt(1, hid);
        stmt.setString(2, type);
        stmt.setDouble(3, latitude);
        stmt.setDouble(4, longitude);
        stmt.setInt(5, aid);
        stmt.setString(6, "ACTIVE");
        stmt.executeUpdate();
        //System.out.println("LISTING CREATED");
        return getListingID(hid, type, latitude, longitude, aid, "ACTIVE");
    }

    public void offerAmenity(int lid, String description) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO Offers VALUES (?, ?)");
        stmt.setInt(1, lid);
        stmt.setString(2, description);
        stmt.executeUpdate();
    }

    public Booking getBooking(int lid, String startDate, String endDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Bookings " +
                "WHERE LID = ? AND StartDate = ? AND endDate = ?");
        stmt.setInt(1, lid);
        stmt.setString(2, startDate);
        stmt.setString(3, endDate);

        ResultSet rs = stmt.executeQuery();
        Booking booking = null;
        if (rs.next()) {
            int bid = rs.getInt("BID");
            int rid = rs.getInt("RID");
            Double cost = rs.getDouble("Cost");
            String status = rs.getString("Status");
            String review = rs.getString("Review");
            int rating = rs.getInt("Rating");
            booking = new Booking(bid, rid, lid, startDate, endDate, cost, status, review, rating);
        }
        return booking;
    }

    public boolean checkAvailability(int lid, String startDate, String endDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT LID FROM Calendars C " +
                "WHERE Status='AVAILABLE' AND LID = ? AND Day BETWEEN ? AND ? " +
                "GROUP BY LID HAVING COUNT(*)=DATEDIFF(?, ?)+1");
        stmt.setInt(1, lid);
        stmt.setString(2, startDate);
        stmt.setString(3, endDate);
        stmt.setString(4, endDate);
        stmt.setString(5, startDate);
        return stmt.executeQuery().next();
    }

    public double getCost(int lid, String startDate, String endDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT SUM(Price) as Cost FROM Calendars " +
                "WHERE LID = ? AND Day BETWEEN ? AND ?");
        stmt.setInt(1, lid);
        stmt.setString(2, startDate);
        stmt.setString(3, endDate);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("Cost");
        }
        return -1;
    }

    public void updateCalendar(int lid, String startDate, String endDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Calendars SET Status = 'BOOKED' " +
                "WHERE LID = ? AND Day BETWEEN ? AND ?");
        stmt.setInt(1, lid);
        stmt.setString(2, startDate);
        stmt.setString(3, endDate);
        stmt.executeUpdate();
    }

    public Booking createBooking(int rid, int lid, String startDate,
                                 String endDate, double cost) throws SQLException {
        // update calendar
        // create a new booking given listing and date range if available
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO " +
                "Bookings(RID, LID, StartDate, EndDate, Cost, Status) VALUES(?, ?, ?, ?, ?, 'UPCOMING')");
        stmt.setInt(1, rid);
        stmt.setInt(2, lid);
        stmt.setString(3, startDate);
        stmt.setString(4, endDate);
        stmt.setDouble(5, cost);
        stmt.executeUpdate();

        return getBooking(lid, startDate, endDate);
    }

    public List<Booking> getRentersBookings(String status, int rid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Bookings WHERE Status = ? AND RID = ?");
        stmt.setString(1, status);
        stmt.setInt(2, rid);
        ResultSet rs = stmt.executeQuery();

        List<Booking> bookings = new ArrayList<Booking>();
        if (rs.next()) {
            int bid = rs.getInt("BID");
            int lid = rs.getInt("LID");
            String startDate = rs.getString("StartDate");
            String endDate = rs.getString("EndDate");
            Double cost = rs.getDouble("Cost");
            String review = rs.getString("Review");
            int rating = rs.getInt("Rating");
            bookings.add(new Booking(bid, rid, lid, startDate, endDate, cost, status, review, rating));
        }
        return bookings;
    }

    public List<Booking> getHostsBookings(String status, int hid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT B.* FROM Bookings B, Listings L " +
                "WHERE B.LID=L.LID AND B.Status = ? AND L.UID = ?");
        stmt.setString(1, status);
        stmt.setInt(2, hid);
        ResultSet rs = stmt.executeQuery();

        List<Booking> bookings = new ArrayList<Booking>();
        if (rs.next()) {
            int bid = rs.getInt("BID");
            int rid = rs.getInt("RID");
            int lid = rs.getInt("LID");
            String startDate = rs.getString("StartDate");
            String endDate = rs.getString("EndDate");
            Double cost = rs.getDouble("Cost");
            String review = rs.getString("Review");
            int rating = rs.getInt("Rating");
            bookings.add(new Booking(bid, rid, lid, startDate, endDate, cost, status, review, rating));
        }
        return bookings;
    }

    public Listing getListingFromID(int lid) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Listings " +
                "NATURAL JOIN Addresses WHERE LID=?");
        stmt.setInt(1, lid);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String type = rs.getString("Type");
            double latitude = rs.getDouble("Latitude");
            double longitude = rs.getDouble("Longitude");

            int aid = rs.getInt("AID");
            String address = rs.getString("Address");
            String city = rs.getString("City");
            String country = rs.getString("Country");
            String postalCode = rs.getString("PostalCode");

            Address newAddress = new Address(aid, address, city, country, postalCode);
            return new Listing(lid, type, latitude, longitude, newAddress);
        }
        return null;
    }

    public void updateBookingStatus() throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE Bookings SET Status='PAST' " +
                "WHERE EndDate < CURDATE()");
        stmt.executeUpdate();
    }

    public boolean deleteUser() {
        return false;
        // remove listings
        // cancel relevant bookings
    }

    public boolean cancelBooking() {
        return false;
        // update calendar
    }

    public boolean removeListing() {
        return false;
        // cancel relevant bookings
    }

    public boolean updatePrice() {
        return false;
        // not allowed if already booked
        // host can change the price of a listing given a date range
    }

    public boolean changeAvailability() {
        return false;
        // host can make an available listing unavailable on a given date
    }

    public boolean reviewBooking() {
        return false;
    }

    public boolean reviewUser() {
        return false;
        // renter or host can leave a rating and/or comment about the other
    }

    public void close() throws SQLException {
        conn.close();
    }
}
