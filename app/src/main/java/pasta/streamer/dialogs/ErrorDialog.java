package pasta.streamer.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.view.View;
import android.widget.TextView;

import pasta.streamer.R;
import pasta.streamer.utils.StaticUtils;

public class ErrorDialog extends AppCompatDialog {

    private String title, message;

    private TextView titleView, messageView;

    public ErrorDialog(Context context) {
        super(context, R.style.AppTheme_Dialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_error);

        titleView = (TextView) findViewById(R.id.title);
        if (title != null) titleView.setText(title);

        messageView = (TextView) findViewById(R.id.message);
        if (message != null) messageView.setText(message);

        findViewById(R.id.restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StaticUtils.restart(getContext());
            }
        });

        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });
    }

    public ErrorDialog setTitle(String title) {
        this.title = title;
        if (titleView != null) titleView.setText(title);
        return this;
    }

    public ErrorDialog setMessage(String message) {
        this.message = message;
        if (messageView != null) messageView.setText(message);
        return this;
    }
}
