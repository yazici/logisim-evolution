/**
 * This file is part of Logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + Haute École Spécialisée Bernoise
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 *   + REDS Institute - HEIG-VD, Yverdon-les-Bains, Switzerland
 *     http://reds.heig-vd.ch
 * This version of the project is currently maintained by:
 *   + Kevin Walsh (kwalsh@holycross.edu, http://mathcs.holycross.edu/~kwalsh)
 */

package com.cburch.logisim.gui.menu;
import static com.cburch.logisim.gui.menu.Strings.S;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.PopupMenu;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.LayoutClipboard;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.gui.main.StatisticsDialog;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Popups {

  private static class CircuitPopup extends PopupMenu {
    Project proj;
    Circuit circuit;

    static final String cut = "projectCutCircuitItem";
    static final String copy = "projectCopyCircuitItem";
    static final String paste = "projectPasteComponentsItem";
    static final String delete = "projectRemoveCircuitItem";

    static final String editLayout = "projectEditCircuitLayoutItem";
    static final String editAppearance = "projectEditCircuitAppearanceItem";
    static final String analyze = "projectAnalyzeCircuitItem";
    static final String stats = "projectGetCircuitStatisticsItem";
    static final String main = "projectSetAsMainItem";

    CircuitPopup(Project proj, Tool tool, Circuit circuit) {
      super(S.get("circuitMenu"));
      this.proj = proj;
      this.circuit = circuit;
      Selection sel = proj.getFrame().getCanvas().getSelection();

      add(cut, S.get(cut), e -> SelectionActions.doCut(proj, circuit));
      add(copy, S.get(copy), e -> SelectionActions.doCopy(proj, circuit));
      add(paste, S.get(paste), e -> SelectionActions.doPasteComponents(proj, sel));
      add(delete, S.get(delete), e -> ProjectCircuitActions.doRemoveCircuit(proj, circuit));

      addSeparator();

      add(editLayout, S.get(editLayout), e -> {
        proj.setCurrentCircuit(circuit);
        proj.getFrame().setEditorView(Frame.EDIT_LAYOUT);
      });
      add(editAppearance, S.get(editAppearance), e -> {
        proj.setCurrentCircuit(circuit);
        proj.getFrame().setEditorView(Frame.EDIT_APPEARANCE);
      });
      add(analyze, S.get(analyze), e -> ProjectCircuitActions.doAnalyze(proj, circuit));
      add(stats, S.get(stats), e -> {
        JFrame frame = (JFrame) SwingUtilities.getRoot(this);
        StatisticsDialog.show(frame, proj.getLogisimFile(), circuit);
      });

      addSeparator();

      add(main, S.get(main), e -> ProjectCircuitActions.doSetAsMainCircuit(proj, circuit));
    }

    @Override
    protected boolean shouldEnable(Object tag) {
      boolean canChange = proj.getLogisimFile().contains(circuit);
      LogisimFile file = proj.getLogisimFile();
      if (tag == cut || tag == delete)
        return canChange && file.getCircuits().size() > 1
          && proj.getDependencies().canRemove(circuit);
      else if (tag == paste)
        return canChange && !LayoutClipboard.forComponents.isEmpty();
      else if (tag == editAppearance)
        return !(circuit == proj.getCurrentCircuit() &&
            proj.getFrame().getEditorView().equals(Frame.EDIT_APPEARANCE));
      else if (tag == editLayout)
        return !(circuit == proj.getCurrentCircuit() &&
            !proj.getFrame().getEditorView().equals(Frame.EDIT_APPEARANCE));
      else if (tag == main)
        return canChange && file.getMainCircuit() != circuit;
      else
        return true;
    }
  }

  private static class VhdlPopup extends PopupMenu {
    Project proj;
    VhdlContent vhdl;

    static final String cut = "projectCutVhdlItem";
    static final String copy = "projectCopyVhdlItem";
    static final String delete = "projectRemoveVhdlItem";

    static final String edit = "projectEditVhdlItem";

    VhdlPopup(Project proj, Tool tool, VhdlContent vhdl) {
      super(S.get("vhdlMenu"));
      this.proj = proj;
      this.vhdl = vhdl;

      add(cut, S.get(cut), e -> SelectionActions.doCut(proj, vhdl));
      add(copy, S.get(copy), e -> SelectionActions.doCopy(proj, vhdl));
      add(delete, S.get(delete), e -> ProjectCircuitActions.doRemoveVhdl(proj, vhdl));

      addSeparator();

      add(edit, S.get(edit), e -> proj.setCurrentHdlModel(vhdl));
    }

    @Override
    protected boolean shouldEnable(Object tag) {
      boolean canChange = proj.getLogisimFile().contains(vhdl);
      LogisimFile file = proj.getLogisimFile();
      if (tag == edit)
        return vhdl != proj.getFrame().getHdlEditorView();
      else if (tag == cut || tag == delete)
        return canChange && proj.getDependencies().canRemove(vhdl);
      else
        return true;
    }
  }

  private static class LibraryPopup extends PopupMenu {
    Project proj;
    Library lib;
    boolean is_top;

    static final String cut = "projectCutLibraryItem";
    static final String copy = "projectCopyLibraryItem";

    static final String unload = "projectUnloadLibraryItem";
    static final String reload = "projectReloadLibraryItem";

    LibraryPopup(Project proj, Library lib, boolean is_top) {
      super(S.get("libMenu"));
      this.proj = proj;
      this.lib = lib;
      this.is_top = is_top;
      LoadedLibrary loadedLib = (lib instanceof LoadedLibrary)
          ? (LoadedLibrary)lib : null;

      add(cut, S.get(cut), e -> SelectionActions.doCut(proj, lib));
      add(copy, S.get(copy), e -> SelectionActions.doCopy(proj, lib));

      addSeparator();

      add(unload, S.get(unload), e -> ProjectLibraryActions.doUnloadLibrary(proj, lib));
      add(reload, S.get(reload), e -> proj.getLogisimFile().getLoader().reload(loadedLib));
    }

    @Override
    protected boolean shouldEnable(Object tag) {
      if (tag == unload || tag == cut)
        return is_top;
      else if (tag == reload)
        return is_top && lib instanceof LoadedLibrary;
      else
        return true;
    }
  }

  private static class ProjectPopup extends PopupMenu {
    Project proj;

    static final String paste = "editPasteItem"; // used only when no others are applicable
    static final String pasteCircuit = "projectPasteCircuitItem";
    static final String pasteAsCircuit = "projectPasteAsCircuitItem";
    static final String pasteVhdl = "projectPasteVhdlItem";
    static final String pasteLibrary = "projectPasteLibraryItem";

    static final String addCirc = "projectAddCircuitItem";
    static final String addVhdl = "projectAddVhdlItem";
    static final String importVhdl = "projectImportVhdlItem";

    JMenu load = new JMenu(S.get("projectLoadLibraryItem"));
    JMenuItem loadBuiltin = new JMenuItem( S.get("projectLoadBuiltinItem"));
    JMenuItem loadLogisim = new JMenuItem( S.get("projectLoadLogisimItem"));
    JMenuItem loadJar = new JMenuItem(S.get("projectLoadJarItem"));

    ProjectPopup(Project proj) {
      super(S.get("projMenu"));
      this.proj = proj;
      Selection sel = proj.getFrame().getCanvas().getSelection();

      add(paste, S.get(paste), e -> { }); // shows as disabled when others paste items are hidden
      add(pasteCircuit, S.get(pasteCircuit), e -> SelectionActions.doPaste(proj, sel));
      add(pasteAsCircuit, S.get(pasteAsCircuit), e -> SelectionActions.doPasteComponentsAsCircuit(proj));
      add(pasteVhdl, S.get(pasteVhdl), e -> SelectionActions.doPaste(proj, sel));
      add(pasteLibrary, S.get(pasteLibrary), e -> SelectionActions.doPaste(proj, sel));

      addSeparator();

      add(addCirc, S.get(addCirc), e -> ProjectCircuitActions.doAddCircuit(proj));
      add(addVhdl, S.get(addVhdl), e -> ProjectCircuitActions.doAddVhdl(proj));
      add(importVhdl, S.get(importVhdl), e -> ProjectCircuitActions.doImportVhdl(proj));

      addSeparator();

      load.add(loadBuiltin);
      loadBuiltin.addActionListener(e -> ProjectLibraryActions.doLoadBuiltinLibrary(proj));
      load.add(loadLogisim);
      loadLogisim.addActionListener(e -> ProjectLibraryActions.doLoadLogisimLibrary(proj));
      load.add(loadJar);
      loadJar.addActionListener(e -> ProjectLibraryActions.doLoadJarLibrary(proj));

      add(load);
    }
    
    @Override
    protected boolean shouldEnable(Object tag) {
      return tag != paste;
    }

    @Override
    protected boolean shouldShow(Object tag) {
      if (tag == pasteCircuit)
        return !LayoutClipboard.forCircuit.isEmpty();
      else if (tag == pasteAsCircuit)
        return !LayoutClipboard.forComponents.isEmpty();
      else if (tag == pasteVhdl)
        return !LayoutClipboard.forVhdl.isEmpty();
      else if (tag == pasteLibrary)
        return !LayoutClipboard.forLibrary.isEmpty();
      else if (tag == paste)
        return LayoutClipboard.forCircuit.isEmpty()
            && LayoutClipboard.forComponents.isEmpty()
            && LayoutClipboard.forVhdl.isEmpty()
            && LayoutClipboard.forLibrary.isEmpty();
      else
        return true;
    }

  }

  public static JPopupMenu forCircuit(Project proj, AddTool tool, Circuit circ) {
    return new CircuitPopup(proj, tool, circ);
  }

  public static JPopupMenu forVhdl(Project proj, AddTool tool, VhdlContent vhdl) {
    return new VhdlPopup(proj, tool, vhdl);
  }

  public static JPopupMenu forLibrary(Project proj, Library lib, boolean isTop) {
    return new LibraryPopup(proj, lib, isTop);
  }

  public static JPopupMenu forProject(Project proj) {
    return new ProjectPopup(proj);
  }

  // todo: tool popup for toolbar?
  // public static JPopupMenu forTool(Project proj, Tool tool) {
  //   return null;
  // }

}
