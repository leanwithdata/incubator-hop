/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.denormaliser;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageDialogWithToggle;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

public class DenormaliserDialog extends BaseTransformDialog implements ITransformDialog {
  private static final Class<?> PKG = DenormaliserMeta.class; // For Translator

  public static final String STRING_SORT_WARNING_PARAMETER = "PivotSortWarning";

  private TableView wGroup;

  private TableView wTarget;

  private CCombo wKeyField;

  private final DenormaliserMeta input;

  private boolean gotPreviousFields = false;

  public DenormaliserDialog(
      Shell parent, IVariables variables, Object in, PipelineMeta pipelineMeta, String sname) {
    super(parent, variables, (BaseTransformMeta) in, pipelineMeta, sname);
    input = (DenormaliserMeta) in;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    props.setLook(shell);
    setShellImage(shell, input);

    ModifyListener lsMod = e -> input.setChanged();
    backupChanged = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "DenormaliserDialog.Shell.Title"));

    int middle = props.getMiddlePct();
    int margin = props.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "DenormaliserDialog.TransformName.Label"));
    props.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    props.setLook(wTransformName);
    wTransformName.addModifyListener(lsMod);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    // Key field...
    Label wlKeyField = new Label(shell, SWT.RIGHT);
    wlKeyField.setText(BaseMessages.getString(PKG, "DenormaliserDialog.KeyField.Label"));
    props.setLook(wlKeyField);
    FormData fdlKeyField = new FormData();
    fdlKeyField.left = new FormAttachment(0, 0);
    fdlKeyField.right = new FormAttachment(middle, -margin);
    fdlKeyField.top = new FormAttachment(wTransformName, margin);
    wlKeyField.setLayoutData(fdlKeyField);

    wKeyField = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
    props.setLook(wKeyField);
    wKeyField.addModifyListener(lsMod);
    FormData fdKeyField = new FormData();
    fdKeyField.left = new FormAttachment(middle, 0);
    fdKeyField.top = new FormAttachment(wTransformName, margin);
    fdKeyField.right = new FormAttachment(100, 0);
    wKeyField.setLayoutData(fdKeyField);

    wKeyField.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseUp(MouseEvent e) {
            Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
            shell.setCursor(busy);
            getPreviousFieldNames();
            shell.setCursor(null);
            busy.dispose();
          }
        });

    Label wlGroup = new Label(shell, SWT.NONE);
    wlGroup.setText(BaseMessages.getString(PKG, "DenormaliserDialog.Group.Label"));
    props.setLook(wlGroup);
    FormData fdlGroup = new FormData();
    fdlGroup.left = new FormAttachment(0, 0);
    fdlGroup.top = new FormAttachment(wKeyField, margin);
    wlGroup.setLayoutData(fdlGroup);

    int nrKeyCols = 1;
    int nrKeyRows = (input.getGroupFields() != null ? input.getGroupFields().size() : 1);

    ColumnInfo[] ciKey = new ColumnInfo[nrKeyCols];
    ciKey[0] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.GroupField"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false);

    wGroup =
        new TableView(
            variables,
            shell,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
            ciKey,
            nrKeyRows,
            lsMod,
            props);

    Button wGet = new Button(shell, SWT.PUSH);
    wGet.setText(BaseMessages.getString(PKG, "DenormaliserDialog.GetFields.Button"));
    FormData fdGet = new FormData();
    fdGet.top = new FormAttachment(wlGroup, margin);
    fdGet.right = new FormAttachment(100, 0);
    wGet.setLayoutData(fdGet);

    FormData fdGroup = new FormData();
    fdGroup.left = new FormAttachment(0, 0);
    fdGroup.top = new FormAttachment(wlGroup, margin);
    fdGroup.right = new FormAttachment(wGet, -margin);
    fdGroup.bottom = new FormAttachment(30, 0);
    wGroup.setLayoutData(fdGroup);

    // THE unpivot target field fields
    Label wlTarget = new Label(shell, SWT.NONE);
    wlTarget.setText(BaseMessages.getString(PKG, "DenormaliserDialog.Target.Label"));
    props.setLook(wlTarget);
    FormData fdlTarget = new FormData();
    fdlTarget.left = new FormAttachment(0, 0);
    fdlTarget.top = new FormAttachment(wGroup, margin);
    wlTarget.setLayoutData(fdlTarget);

    int upInsRows =
        (input.getDenormaliserTargetFields() != null
            ? input.getDenormaliserTargetFields().size()
            : 1);

    ColumnInfo[] ciTarget =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.TargetFieldname"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.ValueFieldname"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Keyvalue"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Type"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              ValueMetaFactory.getAllValueMetaNames(),
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Format"),
              ColumnInfo.COLUMN_TYPE_FORMAT,
              4),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Length"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Precision"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Currency"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Decimal"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Group"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.NullIf"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "DenormaliserDialog.ColumnInfo.Aggregation"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              DenormaliserTargetField.DenormaliseAggregation.getDescriptions(),
              false),
        };

    ciTarget[ciTarget.length - 1].setToolTip(
        BaseMessages.getString(PKG, "DenormaliserDialog.CiTarget.Title"));
    ciTarget[2].setUsingVariables(true);

    wTarget =
        new TableView(
            variables,
            shell,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
            ciTarget,
            upInsRows,
            lsMod,
            props);

    Button wGetAgg = new Button(shell, SWT.PUSH);
    wGetAgg.setText(BaseMessages.getString(PKG, "DenormaliserDialog.GetLookupFields.Button"));
    FormData fdGetAgg = new FormData();
    fdGetAgg.top = new FormAttachment(wlTarget, margin);
    fdGetAgg.right = new FormAttachment(100, 0);
    wGetAgg.setLayoutData(fdGetAgg);

    // THE BUTTONS
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    FormData fdTarget = new FormData();
    fdTarget.left = new FormAttachment(0, 0);
    fdTarget.top = new FormAttachment(wlTarget, margin);
    fdTarget.right = new FormAttachment(wGetAgg, -margin);
    fdTarget.bottom = new FormAttachment(wOk, -margin);
    wTarget.setLayoutData(fdTarget);

    // Add listeners
    wOk.addListener(SWT.Selection, e -> ok());
    wGet.addListener(SWT.Selection, e -> get());
    wGetAgg.addListener(SWT.Selection, e -> getAgg());
    wCancel.addListener(SWT.Selection, e -> cancel());

    getData();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    logDebug(BaseMessages.getString(PKG, "DenormaliserDialog.Log.Getting.KeyInfo"));

    if (input.getKeyField() != null) {
      wKeyField.setText(input.getKeyField());
    }

    if (input.getGroupFields() != null) {
      for (int i = 0; i < input.getGroupFields().size(); i++) {
        TableItem item = wGroup.table.getItem(i);
        if (input.getGroupFields().get(i) != null) {
          DenormaliserGroupField groupfield = input.getGroupFields().get(i);
          item.setText(1, groupfield.getName());
        }
      }
    }

    if (input.getDenormaliserTargetFields() != null) {
      for (int i = 0; i < input.getDenormaliserTargetFields().size(); i++) {
        DenormaliserTargetField field = input.getDenormaliserTargetFields().get(i);

        TableItem item = wTarget.table.getItem(i);

        if (field.getTargetName() != null) {
          item.setText(1, field.getTargetName());
        }
        if (field.getFieldName() != null) {
          item.setText(2, field.getFieldName());
        }
        if (field.getKeyValue() != null) {
          item.setText(3, field.getKeyValue());
        }
        if (field.getTargetType() != null) {
          item.setText(4, field.getTargetType());
        }
        if (field.getTargetFormat() != null) {
          item.setText(5, field.getTargetFormat());
        }
        if (field.getTargetLength() >= 0) {
          item.setText(6, "" + field.getTargetLength());
        }
        if (field.getTargetPrecision() >= 0) {
          item.setText(7, "" + field.getTargetPrecision());
        }
        if (field.getTargetCurrencySymbol() != null) {
          item.setText(8, field.getTargetCurrencySymbol());
        }
        if (field.getTargetDecimalSymbol() != null) {
          item.setText(9, field.getTargetDecimalSymbol());
        }
        if (field.getTargetGroupingSymbol() != null) {
          item.setText(10, field.getTargetGroupingSymbol());
        }
        if (field.getTargetNullString() != null) {
          item.setText(11, field.getTargetNullString());
        }
        if (field.getTargetAggregationType().getDefaultResultType() >= 0) {
          item.setText(12, field.getTargetAggregationType().getDescription());
        }
      }
    }

    wGroup.setRowNums();
    wGroup.optWidth(true);
    wTarget.setRowNums();
    wTarget.optWidth(true);

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    dispose();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    int sizegroup = wGroup.nrNonEmpty();
    int nrFields = wTarget.nrNonEmpty();

    input.setKeyField(wKeyField.getText());
    input.getGroupFields().clear();
    input.getDenormaliserTargetFields().clear();

    for (int i = 0; i < sizegroup; i++) {
      TableItem item = wGroup.getNonEmpty(i);
      // CHECKSTYLE:Indentation:OFF
      DenormaliserGroupField groupfield = new DenormaliserGroupField();
      groupfield.setName(item.getText(1));
      input.getGroupFields().add(groupfield);
    }

    for (int i = 0; i < nrFields; i++) {
      DenormaliserTargetField field = new DenormaliserTargetField();

      TableItem item = wTarget.getNonEmpty(i);
      field.setTargetName(item.getText(1));
      field.setFieldName(item.getText(2));
      field.setKeyValue(item.getText(3));
      field.setTargetType(item.getText(4));
      field.setTargetFormat(item.getText(5));
      field.setTargetLength(Const.toInt(item.getText(6), -1));
      field.setTargetPrecision(Const.toInt(item.getText(7), -1));
      field.setTargetCurrencySymbol(item.getText(8));
      field.setTargetDecimalSymbol(item.getText(9));
      field.setTargetGroupingSymbol(item.getText(10));
      field.setTargetNullString(item.getText(11));
      field.setTargetAggregationType(
          DenormaliserTargetField.DenormaliseAggregation.getTypeWithDescription(item.getText(12)));

      // CHECKSTYLE:Indentation:OFF
      input.getDenormaliserTargetFields().add(field);
    }

    transformName = wTransformName.getText();

    if ("Y".equalsIgnoreCase(props.getCustomParameter(STRING_SORT_WARNING_PARAMETER, "Y"))) {
      MessageDialogWithToggle md =
          new MessageDialogWithToggle(
              shell,
              BaseMessages.getString(PKG, "DenormaliserDialog.Unpivot.DialogTitle"),
              BaseMessages.getString(
                  PKG, "DenormaliserDialog.Unpivot.DialogMessage", Const.CR, Const.CR),
              SWT.ICON_WARNING,
              new String[] {
                BaseMessages.getString(PKG, "DenormaliserDialog.WarningMessage.Option.1")
              },
              BaseMessages.getString(PKG, "DenormaliserDialog.WarningMessage.Option.2"),
              "N".equalsIgnoreCase(props.getCustomParameter(STRING_SORT_WARNING_PARAMETER, "Y")));
      md.open();
      props.setCustomParameter(STRING_SORT_WARNING_PARAMETER, md.getToggleState() ? "N" : "Y");
    }

    dispose();
  }

  private void get() {
    try {
      IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
      if (r != null && !r.isEmpty()) {
        BaseTransformDialog.getFieldsFromPrevious(
            r, wGroup, 1, new int[] {1}, new int[] {}, -1, -1, null);
      }
    } catch (HopException ke) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "DenormaliserDialog.FailedToGetFields.DialogTitle"),
          BaseMessages.getString(PKG, "DenormaliserDialog.FailedToGetFields.DialogMessage"),
          ke);
    }
  }

  private void getAgg() {
    // The grouping fields: ignore those.
    wGroup.removeEmptyRows();
    final String[] groupingFields = wGroup.getItems(0);
    try {
      IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
      if (r != null && !r.isEmpty()) {
        BaseTransformDialog.getFieldsFromPrevious(
            r,
            wTarget,
            2,
            new int[] {},
            new int[] {},
            -1,
            -1,
            (tableItem, v) -> {
              if (Const.indexOfString(v.getName(), groupingFields) < 0
                  && !wKeyField.getText().equalsIgnoreCase(v.getName())) { // Not a grouping field
                // Not the key field
                int nr = tableItem.getParent().indexOf(tableItem) + 1;
                tableItem.setText(
                    1,
                    BaseMessages.getString(PKG, "DenormaliserDialog.TargetFieldname.Label")
                        + nr); // the target fieldname
                tableItem.setText(2, v.getName());
                tableItem.setText(4, v.getTypeDesc());
                if (v.getLength() >= 0) {
                  tableItem.setText(6, "" + v.getLength());
                }
                if (v.getPrecision() >= 0) {
                  tableItem.setText(7, "" + v.getPrecision());
                }
              }
              return true;
            });
      }
    } catch (HopException ke) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "DenormaliserDialog.FailedToGetFields.DialogTitle"),
          BaseMessages.getString(PKG, "DenormaliserDialog.FailedToGetFields.DialogMessage"),
          ke);
    }
  }

  private void getPreviousFieldNames() {
    if (!gotPreviousFields) {
      String keyValue = wKeyField.getText();
      try {
        wKeyField.removeAll();
        IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);

        if (r != null) {
          r.getFieldNames();

          for (int i = 0; i < r.getFieldNames().length; i++) {
            wKeyField.add(r.getFieldNames()[i]);
          }
        }
        if (keyValue != null) {
          wKeyField.setText(keyValue);
        }
        gotPreviousFields = true;
      } catch (HopException ke) {
        if (keyValue != null) {
          wKeyField.setText(keyValue);
        }
        new ErrorDialog(
            shell,
            BaseMessages.getString(PKG, "DenormaliserDialog.FailedToGetFields.DialogTitle"),
            BaseMessages.getString(PKG, "DenormaliserDialog.FailedToGetFields.DialogMessage"),
            ke);
      }
    }
  }
}
