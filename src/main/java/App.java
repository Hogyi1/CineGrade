import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class App extends Application{
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create a simple label
        Label label = new Label("Hello, JavaFX!");

        // Create a scene with the label
        Scene scene = new Scene(label, 400, 300);

        // Set the title and scene for the primary stage
        primaryStage.setTitle("My JavaFX App");
        primaryStage.setScene(scene);

        // Show the primary stage
        primaryStage.show();
    }
}
