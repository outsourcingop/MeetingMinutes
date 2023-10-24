package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TranscribingLinks {
   @SerializedName("files")
   @Expose
   public String files;

   @SerializedName("contentUrl")
   @Expose
   public String contentUrl;
}
