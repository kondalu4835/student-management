package controller;

import dao.DBUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String role = request.getParameter("role");
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (role == null || username == null || password == null ||
                username.isEmpty() || password.isEmpty()) {
            response.sendRedirect("login.html");
            return;
        }

        String table;
        String sql;

        // ✅ Correct SQL based on role
        if ("admin".equals(role)) {
            table = "admins";
            sql = "SELECT * FROM " + table + " WHERE username=? AND password=?";
        } else {
            table = "doctors";
            // doctor login should check doctor_id instead of username
            sql = "SELECT * FROM " + table + " WHERE doctor_id=? AND password=?";
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);  // username field value (entered in form)
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                	// inside rs.next() branch after successful login
                	HttpSession session = request.getSession();

                	if ("admin".equals(role)) {
                	    session.setAttribute("username", rs.getString("username"));
                	    session.setAttribute("fullname", rs.getString("full_name"));
                	    session.setAttribute("role", "admin");
                	    response.sendRedirect("admin/dashboard.jsp");
                	} else {
                	    // doctor
                	    // your DB column is doctor_id (you said you renamed it), so read it:
                	    String doctorIdFromDb = rs.getString("doctor_id"); // e.g. "CD1", "ND2", "GD1"
                	    session.setAttribute("doctor_id", doctorIdFromDb);
                	    session.setAttribute("fullname", rs.getString("full_name"));
                	    session.setAttribute("role", "doctor");
                	    response.sendRedirect("doctor/appointments.html");
                	}

                } else {
                    // invalid login
                    response.sendRedirect("login.html");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("login.html");
        }
    }
}
