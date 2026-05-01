package wtf.choco.aftershock;

import javafx.scene.image.Image;
import wtf.choco.aftershock.util.lazy.LazyValue;

import java.net.URL;

public final class AppResources {

    // FXML resources
    public static final LazyValue<URL> FXML_LAYOUT_ROOT = LazyValue.urlResource("/layout/Root.fxml");
    public static final LazyValue<URL> FXML_LAYOUT_INFO_PANEL = LazyValue.urlResource("/layout/InfoPanel.fxml");
    public static final LazyValue<URL> FXML_LAYOUT_FILTER_POPUP =  LazyValue.urlResource("/layout/FilterPopup.fxml");
    public static final LazyValue<URL> FXML_LAYOUT_SETTINGS_PANEL = LazyValue.urlResource("/layout/SettingsPanel.fxml");
    public static final LazyValue<URL> FXML_COMPONENT_REPLAY_BIN_DISPLAY = LazyValue.urlResource("/component/ReplayBinDisplay.fxml");
    public static final LazyValue<URL> FXML_COMPONENT_REPLAY_BIN_DISPLAY_PANE = LazyValue.urlResource("/component/ReplayBinDisplayPane.fxml");

    // Images
    public static final LazyValue<Image> IMAGE_APP_ICON_64X = LazyValue.resource("/app_icon_64x.png", Image::new);
    public static final LazyValue<Image> IMAGE_ADD = LazyValue.resource("/icons/add.png", Image::new);
    public static final LazyValue<Image> IMAGE_FILE = LazyValue.resource("/icons/file.png", Image::new);
    public static final LazyValue<Image> IMAGE_FOLDER = LazyValue.resource("/icons/folder.png", Image::new);
    public static final LazyValue<Image> IMAGE_FOLDER_FULL = LazyValue.resource("/icons/folder-full.png", Image::new);
    public static final LazyValue<Image> IMAGE_GITHUB_LOGO = LazyValue.resource("/icons/github-logo.png", Image::new);
    public static final LazyValue<Image> IMAGE_PAYPAL_LOGO = LazyValue.resource("/icons/paypal-logo.png", Image::new);
    public static final LazyValue<Image> IMAGE_REMOVE = LazyValue.resource("/icons/remove.png", Image::new);
    public static final LazyValue<Image> IMAGE_SEARCH = LazyValue.resource("/icons/search.png", Image::new);

    private AppResources() { }

}
