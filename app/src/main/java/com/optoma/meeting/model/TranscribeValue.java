package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TranscribeValue {
   @SerializedName("self")
   @Expose
   public String self;

   @SerializedName("contentUrls")
   @Expose
   public List<String> contentUrls = null;

   @SerializedName("model")
   @Expose
   public TranscribingModel model;

   @SerializedName("links")
   @Expose
   public TranscribingLinks links;

   @SerializedName("properties")
   @Expose
   public TranscribeProperties properties;

   @SerializedName("lastActionDateTime")
   @Expose
   public String lastActionDateTime;

   @SerializedName("status")
   @Expose
   public String status;

   @SerializedName("createdDateTime")
   @Expose
   public String createdDateTime;

   @SerializedName("locale")
   @Expose
   public String locale;

   @SerializedName("displayName")
   @Expose
   public String displayName;

   @SerializedName("customProperties")
   @Expose
   public CustomProperties customProperties;

   @SerializedName("name")
   @Expose
   public String name;

   @SerializedName("kind")
   @Expose
   public String kind;
}
