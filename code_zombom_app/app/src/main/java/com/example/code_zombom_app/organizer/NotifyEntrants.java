package com.example.code_zombom_app.organizer;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.net.Uri;

public class NotifyEntrants {
    void sendemail(String useremail){

        String[] to = {useremail};
        String[] cc = {"zipbomapp@gmail.com"};

        Intent mail = new Intent(Intent.ACTION_SEND);
        mail.setData(Uri.parse("mailto:"));
        mail.putExtra(Intent.EXTRA_EMAIL, to);
        mail.putExtra(Intent.EXTRA_CC, cc);
        mail.putExtra(Intent.EXTRA_TEXT, "If this works you can sleep");
        mail.putExtra(Intent.EXTRA_SUBJECT, "Zipbomapp");

        //startActivity(Intent.createChooser(mail, "mailto"));
    }
}
