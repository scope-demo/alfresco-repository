/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.workflow.jbpm;

import java.util.Collection;
import java.util.Map;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

/**
 * Action Handler for ending child tokens / tasks
 * 
 * @author davidc
 */
public class JoinEndForkedTokens implements ActionHandler
{
    private static final long serialVersionUID = 8679390550752208189L;

    /**
     * Constructor
     */
    public JoinEndForkedTokens()
    {
    }

    /* (non-Javadoc)
     * @see org.jbpm.graph.def.ActionHandler#execute(org.jbpm.graph.exe.ExecutionContext)
     */
    public void execute(ExecutionContext executionContext)
    {
        Token token = executionContext.getToken();
        Map childTokens = token.getActiveChildren();
        for (Object childToken : childTokens.values())
        {
            cancelToken(executionContext, (Token)childToken);
        }
    }

    /**
     * Cancel token
     * 
     * @param executionContext
     * @param token
     */
    protected void cancelToken(ExecutionContext executionContext, Token token)
    {
        // visit child tokens
        Map childTokens = token.getActiveChildren();
        for (Object childToken : childTokens.values())
        {
            cancelToken(executionContext, (Token)childToken);
        }

        // end token
        if (!token.hasEnded())
        {
            token.end(false);
        }
        
        // end any associated tasks
        cancelTokenTasks(executionContext, token);
    }

    /**
     * Cancel tasks associated with a token
     * 
     * @param executionContext
     * @param token
     */
    protected void cancelTokenTasks(ExecutionContext executionContext, Token token)
    {
        TaskMgmtInstance tms = executionContext.getTaskMgmtInstance();
        Collection tasks = tms.getUnfinishedTasks(token);
        for (Object task : tasks)
        {
            TaskInstance taskInstance = (TaskInstance)task;
            if (taskInstance.isBlocking())
            {
                taskInstance.setBlocking(false);
            }
            if (taskInstance.isSignalling())
            {
                taskInstance.setSignalling(false);
            }
            if (!taskInstance.hasEnded())
            {
                taskInstance.cancel();
            }
        }
    }
}
