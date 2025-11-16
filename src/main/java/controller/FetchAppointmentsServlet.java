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
import java.util.*;

@WebServlet("/fetchAppointments")
public class FetchAppointmentsServlet extends HttpServlet {

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 🔹 Prevent caching issues
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        Map<String, Object> responseMap = new HashMap<>();

        if (session == null || session.getAttribute("doctor_id") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("error", "Session expired or not logged in.");
            writeJson(response, responseMap);
            return;
        }

        String role = (String) session.getAttribute("role");
        if (role == null || !"doctor".equalsIgnoreCase(role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            responseMap.put("error", "Access denied. Only doctors can view appointments.");
            writeJson(response, responseMap);
            return;
        }

        String doctorId = (String) session.getAttribute("doctor_id");
        List<Map<String, Object>> appointments = new ArrayList<>();

        String sql = "SELECT patient_name, appointment_date, appointment_time, status " +
                     "FROM appointments WHERE doctor_id = ? ORDER BY appointment_date, appointment_time";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("patientName", rs.getString("patient_name"));
                    row.put("appointmentDate", rs.getDate("appointment_date"));
                    row.put("appointmentTime", rs.getString("appointment_time"));
                    row.put("status", rs.getString("status"));
                    appointments.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("error", "Database error: " + e.getMessage());
            writeJson(response, responseMap);
            return;
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(appointments));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void writeJson(HttpServletResponse response, Map<String, Object> data) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(data));
        }
    }
}
