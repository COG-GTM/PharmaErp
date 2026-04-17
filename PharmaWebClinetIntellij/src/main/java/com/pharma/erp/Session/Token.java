package com.pharma.erp.Session;

import com.google.gson.Gson;
import io.jsonwebtoken.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.*;
import java.util.Date;
import java.util.logging.Logger;

@WebServlet(name = "token",urlPatterns = ("/token"))
public class Token extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Token.class.getName());

    protected void doProcess(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, NoSuchAlgorithmException, ClassNotFoundException {

        KeyPair kp = (KeyPair) getServletContext().getAttribute("Key_Pair");
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();

        String authenticatedUser = (String) request.getSession().getAttribute("username");
        if (authenticatedUser == null) {
            authenticatedUser = "anonymous";
        }

        String _jwt = Jwts.builder().setSubject("user/login")
                        .setExpiration(new Date(System.currentTimeMillis()+3600*1000))
                        .claim("username", authenticatedUser)
                        .claim("scope","admin")
                        .signWith(SignatureAlgorithm.RS256, privateKey)
                        .compact();
        response.addCookie(new Cookie("token",_jwt));
        Gson gson = new Gson();
        response.setContentType("application/json");
        response.getWriter().println(gson.toJson(_jwt));

        // Verify token using public key (not private key)
        String _get = gson.fromJson(gson.toJson(_jwt),String.class);
        Jws<Claims> token = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(_get);

        LOGGER.info("Token generated for user: " + authenticatedUser);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            doProcess(request,response);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Token generation error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Token generation error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            doProcess(request,response);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Token generation error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Token generation error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        }
    }
}
