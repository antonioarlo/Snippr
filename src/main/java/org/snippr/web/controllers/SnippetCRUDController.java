/*
 * This file is part of Snippr
 *
 * Copyright (C) 2012 Igalia, S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.snippr.web.controllers;

import java.io.IOException;
import java.util.List;

import org.snippr.business.entities.Snippet;
import org.snippr.business.entities.SnippetCode;
import org.snippr.business.exceptions.DuplicateName;
import org.snippr.business.exceptions.InstanceNotFoundException;
import org.snippr.web.common.Notificator;
import org.snippr.web.common.OnlyOneVisible;
import org.snippr.web.common.Util;
import org.snippr.web.model.ILabelModel;
import org.snippr.web.model.ISnippetModel;
import org.springframework.security.core.Authentication;
import org.zkoss.spring.security.SecurityUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.api.Button;
import org.zkoss.zul.api.Grid;
import org.zkoss.zul.api.Listbox;
import org.zkoss.zul.api.Textbox;
import org.zkoss.zul.api.Window;


/**
 * @author José Manuel Ciges Regueiro <jmanuel@ciges.net>
 * @author Jorge Muñoz Castañer <punkto@gmail.com>
 * @version 20120829
 */
public class SnippetCRUDController extends GenericForwardComposer {

    private Notificator notificator;

    private Label notificationMessage;

    private ISnippetModel snippetModel;

    private Window listWindow;

    private Window renameWindow;

    private Window editWindow;

    private Listbox listSnippets;

    private OnlyOneVisible visibility;

    private boolean isnewSnippet;

    private Listbox listLabels;

    private ILabelModel labelModel;

    /*
     * (non-Javadoc)
     *
     * @see
     * org.zkoss.zk.ui.util.GenericForwardComposer#doAfterCompose(org.zkoss.
     * zk.ui.Component)
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setAttribute("controller", this);
        notificator = Notificator.create(notificationMessage);
    }

    /**
     * Returns the list of Snippets from the database
     * @return List<Snippet>
     */
    public List<Snippet> getSnippets() {
        return snippetModel.getAll();
    }

    /**
     * Returns editing Snippet
     * @return Snippet
     */
    public Snippet getSnippet() {
        return snippetModel.getSnippet();
    }

    /**
     * Sets the label of the actual cell in the grid
     * @param String title
     */
    private void setTitle(String title) {
        Tab tab = (Tab) editWindow.getFellowIfAny("tab");
        if (tab != null) {
            tab.setLabel(title);
        }
    }

    /**
     * Helper function to change between the list windows with the grid and the edit window
     * @return OnlyOneVisible
     */
    private OnlyOneVisible getVisibility() {
        if (visibility == null) {
            visibility = new OnlyOneVisible(listWindow, renameWindow, editWindow);
        }
        return visibility;
    }

    /**
     * Helper function to replace list window by rename window
     * @param String title
     */
    private void showRenameWindow(String title) {
        setTitle(title);
        Util.reloadBindings(renameWindow);
        getVisibility().showOnly(renameWindow);
    }

    /**
     * Opens the form to edit the Snippet title selected
     * @param Snippet snippet
     */
    public void openRenameForm(Snippet snippet) {
        try {
            snippetModel.prepareForEdit(snippet.getId());
            showRenameWindow(String.format("Edit snippet title: %s", snippet.getTitle()));
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        }
        showRenameWindow(String.format("Edit snippet title: %s", snippet.getTitle()));
    }

    /**
     * Delete the snippet selected
     * @param Snippet snippet
     * @throws InstanceNotFoundException
     */
    public void delete(Snippet snippet) throws InstanceNotFoundException {
        snippetModel.delete(snippet);
        Util.reloadBindings(listWindow);
    }

    /**
     * Helper function to show edit window
     * @param String title
     */
    private void showEditWindow(String title) {
        setTitle(title);
        Util.reloadBindings(editWindow);
        getVisibility().showOnly(editWindow);
    }

    /**
     * Opens the form to edit a snippet with its snippet codes
     * @param Snippet snippet
     */
    public void openEditForm(Snippet snippet) {
        try {
            snippetModel.prepareForEdit(snippet.getId());
            showEditWindow("Edit snippet");
        } catch (InstanceNotFoundException e)   {
            e.printStackTrace();
        }
        isnewSnippet = false;
    }

    /**
     * Opens the form to create a snippet with its snippet codes
     * @param Snippet snippet
     */
    public void openCreateForm() {
            snippetModel.prepareForCreate();
            // Disable some buttons and grid while the new snippet is not save
            Button addcodeButton = (Button) editWindow.getFellow("addcodeButton");
            Button saveButton = (Button) editWindow.getFellow("saveButton");
            Button saveandcontinueButton = (Button) editWindow.getFellow("saveandcontinueButton");
            Grid snippetCodeGrid = (Grid) editWindow.getFellow("snippetCodeGrid");
            addcodeButton.setDisabled(true);
            saveButton.setDisabled(true);
            saveandcontinueButton.setDisabled(true);
            snippetCodeGrid.setVisible(false);
            
            showEditWindow("Create a new  snippet");
            isnewSnippet = true;
    }

    /**
     * Redirects to main snippet window in case of operation canceled
     * @throws IOException
     */
    public void cancel() throws IOException {
        Executions.sendRedirect("/snippets/snippets.zul");
    }

    /**
     * Save the snippet and returns to main snippet window
     * @throws IOException
     */
    public void saveAndExit() throws IOException {
        try {
            snippetModel.save();
            cancel();
        } catch (DuplicateName e) {
            notificator.error("Duplicated Snippet");
        }
    }

    /**
     * Save the snippet without returning to main snippet window
     */
    public void saveAndContinue() {
        try {
            snippetModel.save();
            Util.reloadBindings(editWindow);
            notificator.info("Snippet saved");
            if (isnewSnippet)   {
                Button addcodeButton = (Button) editWindow.getFellow("addcodeButton");
                Grid snippetCodeGrid = (Grid) editWindow.getFellow("snippetCodeGrid");
                snippetCodeGrid.setVisible(true);
                addcodeButton.setDisabled(false);
                // Now we go to "edit mode"
                openEditForm(snippetModel.getSnippet());
            }
        } catch (DuplicateName e) {
            notificator.error("Duplicated Snippet");
        }
    }

    /**
     * Add a blank SnippetCode to the snippet
     */
    public void addSnippetCode() {
        snippetModel.addNewSnippetCode();
        try {
            snippetModel.save();
            Util.reloadBindings(editWindow);
            notificator.info("Snippet Code created");
        } catch (DuplicateName e) {
            notificator.error("Duplicated Snippet");
        }
    }

    /**
     * Delete the snippetCode passed as argument
     * @param SnippetCode snippetCode
     */

    public void delSnippetCode(SnippetCode snippetCode) {
        try {
            snippetModel.deleteSnippetCode(snippetCode);
            // Reload data after deleting the snippet code
            snippetModel.prepareForEdit(snippetModel.getSnippet().getId());
            Util.reloadBindings(editWindow);
            notificator.info("Snippet Code deleted");
        } catch (InstanceNotFoundException e)   {
            e.printStackTrace();
        }
    }

    /**
     * Redirects to home window
     * @throws IOException
     */
    public void goHome() throws IOException {
        Executions.sendRedirect("/");
    }

    /**
     * Creates a new Snippet with default data
     */
    public void addSnippet() {
        try {
            snippetModel.addNewSnippet();
        } catch (DuplicateName e) {
            // alert();
            try {
                Messagebox
                        .show("Duplicated Snippet. Please fill the previous default snippet before creating a new one.",
                                "Error", 0,
                        Messagebox.ERROR);
            } catch (InterruptedException e1) {
            }
        }
        Util.reloadBindings(listSnippets);
    }
    
    /**
     * Verify if the user can save the new Snippet for enabling the save buttons
     */
    public void activateSaveButton()    {
        if (isnewSnippet == true)   {
            Textbox titleTextbox = (Textbox) editWindow.getFellow("titleTextbox");
            Textbox descriptionTextbox = (Textbox) editWindow.getFellow("descriptionTextbox");
            Button saveButton = (Button) editWindow.getFellow("saveButton");
            Button saveandcontinueButton = (Button) editWindow.getFellow("saveandcontinueButton");
            if (titleTextbox.getValue().trim().isEmpty() || descriptionTextbox.getValue().trim().isEmpty()) {
                saveButton.setDisabled(true);
                saveandcontinueButton.setDisabled(true);
            }
            else    {
                saveButton.setDisabled(false);
                saveandcontinueButton.setDisabled(false);
            }
        }
    }

    /**
     * Returns the list of all  Labels from the database
     * @return List<Snippet>
     */
    public List<org.snippr.business.entities.Label> getLabels() {
        Authentication auth = SecurityUtil.getAuthentication();
        String username = auth.getName();
        // TODO:   The labels got should be only the labels for the authentified user
        //          (With a code like return userModel.getLabels())
        return labelModel.getAll();
    }

}
