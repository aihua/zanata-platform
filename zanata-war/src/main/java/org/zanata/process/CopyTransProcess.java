/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.process;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.zanata.service.CopyTransService;
import org.zanata.service.impl.CopyTransServiceImpl;

/**
 * Performs copy trans as a background process.
 *
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class CopyTransProcess extends RunnableProcess<CopyTransProcessHandle>
{
   @Override
   protected void run(CopyTransProcessHandle handle) throws Throwable
   {
      CopyTransService copyTransServiceImpl =
            (CopyTransService)Component.getInstance(CopyTransServiceImpl.class);

      if( handle.getProjectIteration() != null )
      {
         copyTransServiceImpl.copyTransForIteration( handle.getProjectIteration() );
      }
      else
      {
         copyTransServiceImpl.copyTransForDocument( handle.getDocument() );
      }
   }
}
