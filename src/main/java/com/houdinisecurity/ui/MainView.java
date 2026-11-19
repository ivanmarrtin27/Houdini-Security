package com.houdinisecurity.ui;

import com.houdinisecurity.AppContext;
import com.houdinisecurity.crypto.KdfAlgorithm;
import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.domain.ApiKeyEntry;
import com.houdinisecurity.domain.Folder;
import com.houdinisecurity.domain.PasswordEntry;
import com.houdinisecurity.domain.SecureNote;
import com.houdinisecurity.domain.Tag;
import com.houdinisecurity.domain.VaultEntry;
import com.houdinisecurity.service.AuditService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public final class MainView {

    private final AppContext ctx;
    private final ViewNavigator navigator;

    private final ListView<VaultEntry> entryList = new ListView<>();
    private final ListView<String> sidebar = new ListView<>();
    private final TextField search = new TextField();
    private final VBox detail = new VBox(10);
    private String filterFolderId;
    private String filterTagId;
    private boolean favoritesOnly;

    public MainView(AppContext ctx, ViewNavigator navigator) {
        this.ctx = ctx;
        this.navigator = navigator;
    }

    public BorderPane create() {
        I18n i18n = ctx.i18n();
        rebuildSidebar();
        refreshList();

        search.setPromptText(i18n.t("main.search"));
        search.textProperty().addListener((o, a, b) -> refreshList());

        sidebar.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> onSidebar(b));
        entryList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(VaultEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayTitle() + (item.isFavorite() ? " ★" : ""));
            }
        });
        entryList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));

        Button addPassword = new Button(i18n.t("entry.addPassword"));
        Button addNote = new Button(i18n.t("entry.addNote"));
        Button addApi = new Button(i18n.t("entry.addApi"));
        addPassword.setOnAction(e -> editPassword(null));
        addNote.setOnAction(e -> editNote(null));
        addApi.setOnAction(e -> editApi(null));

        HBox toolbar = new HBox(8, search, addPassword, addNote, addApi);
        HBox.setHgrow(search, Priority.ALWAYS);
        toolbar.setPadding(new Insets(8));

        detail.setPadding(new Insets(12));
        detail.getChildren().add(new Label(i18n.t("main.selectEntry")));

        VBox center = new VBox(entryList);
        VBox.setVgrow(entryList, Priority.ALWAYS);
        SplitPane split = new SplitPane(sidebar, center, detail);
        split.setDividerPositions(0.18, 0.48);

        BorderPane root = new BorderPane();
        root.setTop(new VBox(buildMenu(), toolbar));
        root.setCenter(split);
        root.getStyleClass().add("root-pane");

        root.setOnKeyPressed(e -> {
            ctx.autolockService().bumpActivity();
            if (new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN).match(e)) {
                ctx.autolockService().lockNow();
            }
        });
        root.setOnMouseMoved(e -> ctx.autolockService().bumpActivity());
        return root;
    }

    private MenuBar buildMenu() {
        I18n i18n = ctx.i18n();
        Menu vault = new Menu(i18n.t("menu.vault"));
        MenuItem lock = new MenuItem(i18n.t("menu.lock"));
        lock.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
        lock.setOnAction(e -> ctx.autolockService().lockNow());
        MenuItem changeMaster = new MenuItem(i18n.t("menu.changeMaster"));
        changeMaster.setOnAction(e -> changeMaster());
        MenuItem exportBak = new MenuItem(i18n.t("menu.exportBackup"));
        exportBak.setOnAction(e -> exportBackup());
        MenuItem importCsv = new MenuItem(i18n.t("menu.importCsv"));
        importCsv.setOnAction(e -> importCsv());
        vault.getItems().addAll(lock, changeMaster, new SeparatorMenuItem(), exportBak, importCsv);

        Menu tools = new Menu(i18n.t("menu.tools"));
        MenuItem gen = new MenuItem(i18n.t("menu.generator"));
        gen.setOnAction(e -> new GeneratorDialog(ctx).show());
        MenuItem audit = new MenuItem(i18n.t("menu.audit"));
        audit.setOnAction(e -> showAudit());
        MenuItem settings = new MenuItem(i18n.t("menu.settings"));
        settings.setOnAction(e -> showSettings());
        tools.getItems().addAll(gen, audit, settings);

        Menu org = new Menu(i18n.t("menu.org"));
        MenuItem addFolder = new MenuItem(i18n.t("menu.addFolder"));
        addFolder.setOnAction(e -> addFolder());
        MenuItem addTag = new MenuItem(i18n.t("menu.addTag"));
        addTag.setOnAction(e -> addTag());
        org.getItems().addAll(addFolder, addTag);

        return new MenuBar(vault, tools, org);
    }

    private void rebuildSidebar() {
        I18n i18n = ctx.i18n();
        sidebar.getItems().clear();
        sidebar.getItems().add(i18n.t("sidebar.all"));
        sidebar.getItems().add(i18n.t("sidebar.favorites"));
        sidebar.getItems().add("--- " + i18n.t("sidebar.folders"));
        for (Folder f : ctx.vaultService().listFolders()) {
            sidebar.getItems().add("F:" + f.getName() + "|" + f.getId());
        }
        sidebar.getItems().add("--- " + i18n.t("sidebar.tags"));
        for (Tag t : ctx.vaultService().listTags()) {
            sidebar.getItems().add("T:" + t.getName() + "|" + t.getId());
        }
        sidebar.getSelectionModel().select(0);
    }

    private void onSidebar(String item) {
        if (item == null || item.startsWith("---")) {
            return;
        }
        I18n i18n = ctx.i18n();
        filterFolderId = null;
        filterTagId = null;
        favoritesOnly = false;
        if (item.equals(i18n.t("sidebar.favorites"))) {
            favoritesOnly = true;
        } else if (item.startsWith("F:")) {
            filterFolderId = item.substring(item.lastIndexOf('|') + 1);
        } else if (item.startsWith("T:")) {
            filterTagId = item.substring(item.lastIndexOf('|') + 1);
        }
        refreshList();
    }

    private void refreshList() {
        List<VaultEntry> items = ctx.vaultService().search(search.getText(), filterFolderId, filterTagId, favoritesOnly);
        entryList.setItems(FXCollections.observableArrayList(items));
    }

    private void showDetail(VaultEntry entry) {
        detail.getChildren().clear();
        I18n i18n = ctx.i18n();
        if (entry == null) {
            detail.getChildren().add(new Label(i18n.t("main.selectEntry")));
            return;
        }
        Label title = new Label(entry.displayTitle());
        title.getStyleClass().add("detail-title");
        CheckBox fav = new CheckBox(i18n.t("entry.favorite"));
        fav.setSelected(entry.isFavorite());
        fav.setOnAction(e -> {
            entry.setFavorite(fav.isSelected());
            ctx.vaultService().saveEntry(entry);
            refreshList();
        });
        Button edit = new Button(i18n.t("action.edit"));
        Button delete = new Button(i18n.t("action.delete"));
        delete.getStyleClass().add("danger");
        edit.setOnAction(e -> {
            if (entry instanceof PasswordEntry p) {
                editPassword(p);
            } else if (entry instanceof SecureNote n) {
                editNote(n);
            } else if (entry instanceof ApiKeyEntry a) {
                editApi(a);
            }
        });
        delete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, i18n.t("confirm.delete"));
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    ctx.vaultService().deleteEntry(entry.getId());
                    refreshList();
                    detail.getChildren().setAll(new Label(i18n.t("main.selectEntry")));
                }
            });
        });

        detail.getChildren().addAll(title, fav);
        if (entry instanceof PasswordEntry p) {
            detail.getChildren().add(maskedRow(i18n.t("field.username"), p.getUsername(), false));
            char[] pw = p.getPasswordCopy();
            try {
                detail.getChildren().add(maskedRow(i18n.t("field.password"), new String(pw), true));
            } finally {
                SecureMemory.wipe(pw);
            }
            String urlVal = p.getUrl() == null ? "" : p.getUrl();
            if (!urlVal.isBlank()) {
                detail.getChildren().add(maskedRow(i18n.t("field.url"), urlVal, false));
            }
            char[] codes = p.getRecoveryCodesCopy();
            try {
                if (codes.length > 0) {
                    detail.getChildren().add(multilineSecretRow(
                            i18n.t("field.recoveryCodes"), new String(codes)));
                }
            } finally {
                SecureMemory.wipe(codes);
            }
            if (p.getNotes() != null && !p.getNotes().isBlank()) {
                Label notesLabel = new Label(i18n.t("field.notes") + ":");
                notesLabel.getStyleClass().add("form-label");
                TextArea notesView = new TextArea(p.getNotes());
                notesView.setEditable(false);
                notesView.setWrapText(true);
                notesView.setPrefRowCount(3);
                detail.getChildren().addAll(notesLabel, notesView);
            }
        } else if (entry instanceof SecureNote n) {
            TextArea body = new TextArea(n.getBody());
            body.setEditable(false);
            body.setWrapText(true);
            detail.getChildren().add(body);
        } else if (entry instanceof ApiKeyEntry a) {
            char[] key = a.getApiKeyCopy();
            char[] secret = a.getApiSecretCopy();
            try {
                detail.getChildren().add(maskedRow(i18n.t("field.apiKey"), new String(key), true));
                detail.getChildren().add(maskedRow(i18n.t("field.apiSecret"), new String(secret), true));
            } finally {
                SecureMemory.wipe(key);
                SecureMemory.wipe(secret);
            }
            detail.getChildren().add(new Label(i18n.t("field.expires") + ": "
                    + (a.getExpiresAt() == null ? "-" : Instant.ofEpochMilli(a.getExpiresAt()).toString())));
        }
        detail.getChildren().add(new HBox(8, edit, delete));
    }

    private HBox maskedRow(String label, String value, boolean secret) {
        I18n i18n = ctx.i18n();
        Label l = new Label(label + ":");
        l.getStyleClass().add("form-label");
        Label masked = new Label(secret ? "••••••••" : value);
        masked.setWrapText(true);
        Button reveal = new Button(i18n.t("action.reveal"));
        Button copy = new Button(i18n.t("action.copy"));
        final boolean[] shown = {false};
        reveal.setOnAction(e -> {
            shown[0] = !shown[0];
            masked.setText(shown[0] ? value : (secret ? "••••••••" : value));
            reveal.setText(shown[0] ? i18n.t("action.hide") : i18n.t("action.reveal"));
        });
        copy.setOnAction(e -> ctx.clipboardService().copy(value, ctx.vaultService().settings().getClipboardTimeoutSec()));
        HBox row = new HBox(8, l, masked, reveal, copy);
        HBox.setHgrow(masked, Priority.ALWAYS);
        return row;
    }

    private VBox multilineSecretRow(String label, String value) {
        I18n i18n = ctx.i18n();
        Label l = new Label(label + ":");
        l.getStyleClass().add("form-label");
        TextArea area = new TextArea("••••••••");
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(4);
        area.getStyleClass().add("recovery-codes");
        Button reveal = new Button(i18n.t("action.reveal"));
        Button copy = new Button(i18n.t("action.copy"));
        final boolean[] shown = {false};
        reveal.setOnAction(e -> {
            shown[0] = !shown[0];
            area.setText(shown[0] ? value : "••••••••");
            reveal.setText(shown[0] ? i18n.t("action.hide") : i18n.t("action.reveal"));
        });
        copy.setOnAction(e -> ctx.clipboardService().copy(value, ctx.vaultService().settings().getClipboardTimeoutSec()));
        return new VBox(6, l, area, new HBox(8, reveal, copy));
    }

    private void editPassword(PasswordEntry existing) {
        new PasswordEntryDialog(ctx, existing).showAndWait().ifPresent(entry -> {
            ctx.vaultService().saveEntry(entry);
            refreshList();
            entryList.getSelectionModel().select(entry);
            showDetail(entry);
        });
    }

    private void editNote(SecureNote existing) {
        I18n i18n = ctx.i18n();
        DialogForm form = new DialogForm(existing == null ? i18n.t("entry.addNote") : i18n.t("action.edit"));
        TextField title = form.addText(i18n.t("field.title"), existing == null ? "" : existing.getTitle());
        TextArea body = form.addArea(i18n.t("field.body"), existing == null ? "" : existing.getBody());
        form.showAndWait().ifPresent(ok -> {
            SecureNote entry = existing == null ? SecureNote.create(title.getText(), body.getText()) : existing;
            if (existing != null) {
                existing.setTitle(title.getText());
                existing.setBody(body.getText());
            }
            ctx.vaultService().saveEntry(entry);
            refreshList();
        });
    }

    private void editApi(ApiKeyEntry existing) {
        I18n i18n = ctx.i18n();
        DialogForm form = new DialogForm(existing == null ? i18n.t("entry.addApi") : i18n.t("action.edit"));
        TextField service = form.addText(i18n.t("field.service"), existing == null ? "" : existing.getService());
        PasswordField key = form.addPassword(i18n.t("field.apiKey"));
        PasswordField secret = form.addPassword(i18n.t("field.apiSecret"));
        TextField expires = form.addText(i18n.t("field.expires") + " (YYYY-MM-DD)", "");
        if (existing != null) {
            char[] k = existing.getApiKeyCopy();
            char[] s = existing.getApiSecretCopy();
            try {
                key.setText(new String(k));
                secret.setText(new String(s));
            } finally {
                SecureMemory.wipe(k);
                SecureMemory.wipe(s);
            }
            if (existing.getExpiresAt() != null) {
                expires.setText(LocalDate.ofInstant(Instant.ofEpochMilli(existing.getExpiresAt()), ZoneId.systemDefault()).toString());
            }
        }
        form.showAndWait().ifPresent(ok -> {
            char[] k = key.getText().toCharArray();
            char[] s = secret.getText().toCharArray();
            try {
                Long exp = null;
                if (!expires.getText().isBlank()) {
                    exp = LocalDate.parse(expires.getText()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
                ApiKeyEntry entry = existing == null
                        ? ApiKeyEntry.create(service.getText(), k, s, exp)
                        : existing;
                if (existing != null) {
                    existing.setService(service.getText());
                    existing.setApiKey(k);
                    existing.setApiSecret(s);
                    existing.setExpiresAt(exp);
                }
                ctx.vaultService().saveEntry(entry);
                refreshList();
            } finally {
                SecureMemory.wipe(k);
                SecureMemory.wipe(s);
                key.clear();
                secret.clear();
            }
        });
    }

    private void addFolder() {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText(ctx.i18n().t("menu.addFolder"));
        d.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                ctx.vaultService().saveFolder(Folder.create(name.trim(), null));
                rebuildSidebar();
            }
        });
    }

    private void addTag() {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText(ctx.i18n().t("menu.addTag"));
        d.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                ctx.vaultService().saveTag(Tag.create(name.trim()));
                rebuildSidebar();
            }
        });
    }

    private void showAudit() {
        I18n i18n = ctx.i18n();
        var report = ctx.auditService().audit(
                ctx.vaultService().listEntries(),
                ctx.vaultService().settings().getAuditOldDays(),
                ctx.vaultService().settings().getApiExpiryWarnDays(),
                System.currentTimeMillis());
        StringBuilder sb = new StringBuilder(i18n.t("audit.score", report.score0to100())).append("\n\n");
        for (AuditService.AuditIssue issue : report.issues()) {
            sb.append("• ").append(issue.title()).append(" — ").append(i18n.t(issue.detailKey())).append('\n');
        }
        if (report.issues().isEmpty()) {
            sb.append(i18n.t("audit.none"));
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString());
        a.setHeaderText(i18n.t("menu.audit"));
        a.setResizable(true);
        a.showAndWait();
    }

    private void showSettings() {
        I18n i18n = ctx.i18n();
        DialogForm form = new DialogForm(i18n.t("menu.settings"));
        ComboBox<String> theme = new ComboBox<>(FXCollections.observableArrayList("dark", "light"));
        theme.setValue(ctx.vaultService().settings().getTheme());
        form.addNode(i18n.t("settings.theme"), theme);
        TextField lock = form.addText(i18n.t("settings.lockSec"),
                String.valueOf(ctx.vaultService().settings().getLockTimeoutSec()));
        TextField clip = form.addText(i18n.t("settings.clipboardSec"),
                String.valueOf(ctx.vaultService().settings().getClipboardTimeoutSec()));
        TextField old = form.addText(i18n.t("settings.oldDays"),
                String.valueOf(ctx.vaultService().settings().getAuditOldDays()));
        form.showAndWait().ifPresent(ok -> {
            ctx.vaultService().settings().setTheme(theme.getValue());
            ctx.vaultService().settings().setLockTimeoutSec(Integer.parseInt(lock.getText()));
            ctx.vaultService().settings().setClipboardTimeoutSec(Integer.parseInt(clip.getText()));
            ctx.vaultService().settings().setAuditOldDays(Integer.parseInt(old.getText()));
            ctx.vaultService().persistSettings();
            ThemeManager.apply(navigator.stage().getScene(), theme.getValue());
        });
    }

    private void changeMaster() {
        I18n i18n = ctx.i18n();
        DialogForm form = new DialogForm(i18n.t("menu.changeMaster"));
        PasswordField current = form.addPassword(i18n.t("unlock.master"));
        PasswordField neu = form.addPassword(i18n.t("welcome.master"));
        PasswordField conf = form.addPassword(i18n.t("welcome.confirm"));
        form.showAndWait().ifPresent(ok -> {
            char[] c = current.getText().toCharArray();
            char[] n = neu.getText().toCharArray();
            char[] f = conf.getText().toCharArray();
            try {
                if (!java.util.Arrays.equals(n, f) || n.length < 8) {
                    alert(i18n.t("error.masterMismatch"));
                    return;
                }
                ctx.vaultService().changeMasterPassword(c, n, KdfAlgorithm.ARGON2ID);
                alert(i18n.t("master.changed"));
            } catch (Exception ex) {
                alert(i18n.t("error.generic") + ": " + ex.getMessage());
            } finally {
                SecureMemory.wipe(c);
                SecureMemory.wipe(n);
                SecureMemory.wipe(f);
            }
        });
    }

    private void exportBackup() {
        I18n i18n = ctx.i18n();
        Path header = ctx.vaultService().currentHeaderPath();
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup", "*.hbak"));
        File out = chooser.showSaveDialog(navigator.stage());
        if (out == null) {
            return;
        }
        new PasswordDialog(i18n.t("backup.passphrase"), i18n).showAndWait().ifPresent(pass -> {
            char[] chars = pass.toCharArray();
            try {
                Path target = out.toPath();
                if (!target.toString().endsWith(".hbak")) {
                    target = Path.of(target + ".hbak");
                }
                ctx.backupService().exportBackup(header, chars, target);
                alert(i18n.t("backup.exportOk"));
            } catch (Exception ex) {
                alert(ex.getMessage());
            } finally {
                SecureMemory.wipe(chars);
            }
        });
    }

    private void importCsv() {
        I18n i18n = ctx.i18n();
        Alert warn = new Alert(Alert.AlertType.WARNING, i18n.t("csv.warning"), ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> res = warn.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showOpenDialog(navigator.stage());
        if (file == null) {
            return;
        }
        try {
            for (PasswordEntry e : ctx.csvImportService().importPasswords(file.toPath())) {
                ctx.vaultService().saveEntry(e);
            }
            refreshList();
            alert(i18n.t("csv.importOk"));
        } catch (Exception ex) {
            alert(ex.getMessage());
        }
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /** Small form helper. */
    static final class DialogForm {
        private final javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        private final GridPane grid = new GridPane();
        private int row;

        DialogForm(String title) {
            dialog.setTitle(title);
            grid.setHgap(8);
            grid.setVgap(8);
            grid.setPadding(new Insets(12));
            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(bt -> bt == ButtonType.OK);
        }

        TextField addText(String label, String value) {
            TextField tf = new TextField(value);
            grid.addRow(row++, new Label(label), tf);
            return tf;
        }

        PasswordField addPassword(String label) {
            PasswordField pf = new PasswordField();
            grid.addRow(row++, new Label(label), pf);
            return pf;
        }

        TextArea addArea(String label, String value) {
            TextArea ta = new TextArea(value);
            ta.setPrefRowCount(4);
            grid.addRow(row++, new Label(label), ta);
            return ta;
        }

        void addNode(String label, javafx.scene.Node node) {
            grid.addRow(row++, new Label(label), node);
        }

        Optional<Boolean> showAndWait() {
            return dialog.showAndWait().filter(Boolean::booleanValue);
        }
    }
}
