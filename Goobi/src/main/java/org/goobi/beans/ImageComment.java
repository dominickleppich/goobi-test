package org.goobi.beans;

import java.util.Date;

import lombok.AllArgsConstructor;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://www.intranda.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 * 
 */

import lombok.Data;

@Data
@AllArgsConstructor
public class ImageComment {

    private Integer processId;
    private String comment;
    private String imageName;
    private String imageFolder;
    private Date creationDate;
    private String userName;
    private String step;
    private String location;
    
    @Deprecated(forRemoval = true, since = "2024-02-12")
    public ImageComment(String imageFolder, String imageName, String comment) {

        this.imageFolder = imageFolder;
        this.imageName = imageName;
        this.comment = comment;
    }

}
