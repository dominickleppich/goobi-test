/*
 * This file is part of the Goobi Application - a Workflow tool for the support of
 * mass digitization.
 *
 * Visit the websites for more information.
 *     - http://gdz.sub.uni-goettingen.de
 *     - http://www.goobi.org
 *     - http://launchpad.net/goobi-production
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */

package de.sub.goobi.persistence;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;


//TODO: Fix for Hibernate-Session-Management, old Version reactivated

public class HibernateSessionLong {
   private static final Logger mylogger = Logger.getLogger(HibernateSessionLong.class);

   protected static SessionFactory factory;
   private Session sess;

   /*===============================================================*/

   /**
    * ONLY ever call this method from within the context of a servlet request
    * (specifically, one that has been associated with this filter).  If you
    * want a Hibernate session at some other time, call getSessionFactory()
    * and open/close the session yourself.
    *
    * @return an appropriate Session object
    */
   public Session getSession() throws HibernateException {
    
      if (sess == null) {
         if (factory == null) {
            mylogger.debug("getSession() - hibernate-Factory initialisieren", null);
            factory = new Configuration().configure().buildSessionFactory();
         }
         mylogger.debug("getSession() - hibernate-Session initialisieren", null);
         sess = factory.openSession();
      
      }
    
      return sess;
   }

   /*===============================================================*/

   /**
    * @return the hibernate session factory
    */
   public static SessionFactory getSessionFactory() {
      if (factory == null) {
         mylogger.debug("getSessionFactory() - hibernate-Factory initialisieren", null);
         factory = new Configuration().configure().buildSessionFactory();
      }
      return factory;
   }

   /*===============================================================*/

   /**
    * This is a simple method to reduce the amount of code that needs
    * to be written every time hibernate is used.
    */
   public static void rollback(Transaction tx) {
      if (tx != null) {
         try {
            tx.rollback();
         } catch (HibernateException ex) {
            // Probably don't need to do anything - this is likely being
            // called because of another exception, and we don't want to
            // mask it with yet another exception.
         }
      }
   }

}

