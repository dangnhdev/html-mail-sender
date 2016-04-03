package com.hoasao.dangnh.jfx.utils;

import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Created by dangg on 3/31/2016.
 */
public class MailServerPropertiesFactory{
    private static final Logger logger = Logger.getLogger(MailServerPropertiesFactory.class);
    private static Properties msProps;

    private MailServerPropertiesFactory(){}

    public static synchronized Properties getInstance(){
        if (msProps == null){
            msProps = System.getProperties();
            msProps.put("mail.smtp.port", "587");
            msProps.put("mail.smtp.auth", "true");
            msProps.put("mail.smtp.starttls.enable", "true");
            logger.info("Mail Server Properties have been set up successfully");
        }
        return msProps;
    }
}
