package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TranscribeBody {
   @SerializedName("contentUrls")
   @Expose
   public List<String> contentUrls = null;
   @SerializedName("properties")
   @Expose
   public TranscribeProperties properties;
   @SerializedName("locale")
   @Expose
   public String locale;
   @SerializedName("displayName")
   @Expose
   public String displayName;

   public TranscribeBody(List<String> contentUrls, TranscribeProperties properties, String locale, String displayName) {
      this.contentUrls = contentUrls;
      this.properties = properties;
      this.locale = locale;
      this.displayName = displayName;
   }
}
