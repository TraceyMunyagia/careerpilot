package com.careerpilot.controller;

import com.careerpilot.dto.CurrentUserView;
import com.careerpilot.exception.ApplicationNotFoundException;
import com.careerpilot.exception.BusinessRuleException;
import com.careerpilot.exception.CareerPilotException;
import com.careerpilot.exception.InvalidStatusTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Turns exceptions thrown anywhere in a controller or service into friendly error pages.
 * Controllers and services just throw; this class decides what the user sees.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ModelAndView handleNotFound(ApplicationNotFoundException ex, HttpServletRequest request) {
        return errorView("error/404", HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidStatusTransitionException.class, BusinessRuleException.class})
    public ModelAndView handleBusinessRule(CareerPilotException ex, HttpServletRequest request) {
        return errorView("error/4xx", HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** e.g. /applications/abc or ?status=BANANA */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleBadParameter(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "'" + ex.getValue() + "' is not a valid value for " + ex.getName() + ".";
        return errorView("error/4xx", HttpStatus.BAD_REQUEST, message, request);
    }

    private ModelAndView errorView(String view, HttpStatus status, String message, HttpServletRequest request) {
        log.info("{} {} -> {} ({})", request.getMethod(), request.getRequestURI(), status.value(), message);

        ModelAndView mav = new ModelAndView(view, status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("path", request.getRequestURI());
        mav.addObject("currentUser", CurrentUserView.from(SecurityContextHolder.getContext().getAuthentication()));
        return mav;
    }
}