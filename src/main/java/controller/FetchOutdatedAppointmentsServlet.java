package controller;

import dao.DBUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@WebServlet("/fetchOutdatedAppointments")
public class FetchOutdatedAppointmentsServlet extends HttpServlet {

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("doctor_id") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Session expired. Please login again.");
            writeJson(response, error);
            return;
        }

        String doctorId = (String) session.getAttribute("doctor_id");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Map<String, Object>> outdatedAppointments = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection()) {

            // ✅ Step 1: Auto-update outdated appointments to "Completed"
            String updateSql = "UPDATE appointments " +
                               "SET status = 'Completed' " +
                               "WHERE doctor_id = ? " +
                               "AND (appointment_date < ? " +
                               "OR (appointment_date = ? AND STR_TO_DATE(appointment_time, '%h:%i %p') < ?)) " +
                               "AND status != 'Completed'";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, doctorId);
                ps.setDate(2, Date.valueOf(today));
                ps.setDate(3, Date.valueOf(today));
                ps.setTime(4, Time.valueOf(now));
                ps.executeUpdate();
            }

            // ✅ Step 2: Fetch all completed (outdated) appointments
            String selectSql = "SELECT patient_name, appointment_date, appointment_time, status " +
                               "FROM appointments " +
                               "WHERE doctor_id = ? AND status = 'Completed' " +
                               "ORDER BY appointment_date DESC, appointment_time DESC";

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, doctorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("patientName", rs.getString("patient_name"));
                        row.put("appointmentDate", rs.getDate("appointment_date"));
                        row.put("appointmentTime", rs.getString("appointment_time"));
                        row.put("status", rs.getString("status"));
                        outdatedAppointments.add(row);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Database error: " + e.getMessage());
            writeJson(response, error);
            return;
        }

        // ✅ Step 3: Return result as JSON
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(outdatedAppointments));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    // 🔹 Helper method to send JSON response
    private void writeJson(HttpServletResponse response, Map<String, Object> data) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(data));
        }
    }
}