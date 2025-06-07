package com.ning.Model;

import com.ning.Controller.mainViewController;
import com.ning.Entity.tbSqlMonitor;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.scene.control.cell.PropertyValueFactory;


public class initTbView {

    public static void init(mainViewController mvc) {

        mvc.getTbc_ID().setCellValueFactory(new PropertyValueFactory<>("index"));
        mvc.getTbc_ID().setStyle("-fx-alignment: CENTER;");
        mvc.getTbc_DATE().setCellValueFactory(new PropertyValueFactory<>("date"));

        // SQL列：绑定数据 + 自动换行 + 悬停显示完整内容
        mvc.getTbc_SQL().setCellValueFactory(new PropertyValueFactory<>("sql"));
        mvc.getTbc_SQL().setCellFactory(column -> {

            TableCell<tbSqlMonitor, String> cell = new TableCell<>();
            Text text = new Text();
            cell.setGraphic(text); // 使用Text控件支持换行
            text.setStyle("-fx-font-family: -apple-system;");

            text.wrappingWidthProperty().bind(mvc.getTbc_SQL().widthProperty());


            cell.itemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.isEmpty()) {
                    text.setText(null);
                    cell.setTooltip(null);
                } else {
                    text.setText(newVal.replace("\n", "↵\n"));

                    Tooltip tooltip = new Tooltip(newVal);
                    tooltip.setStyle("-fx-font-family: -apple-system; -fx-font-size: 12px;");
                    tooltip.setMaxWidth(600); // 限制宽度避免过宽
                    tooltip.setWrapText(true); // 允许Tooltip内换行
                    cell.setTooltip(tooltip);
                }
            });

            cell.setPrefHeight(Control.USE_COMPUTED_SIZE); // 单元格高度随内容自动调整
            return cell;
        });

        mvc.getTbc_SQL().setMinWidth(200);
    }
}