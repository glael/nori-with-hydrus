/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.text.TextUtils;

import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/** {@link io.github.tjg1.library.norilib.clients.SearchClient} for the E621 imageboard. */
public class E621 extends DanbooruLegacy {

  /** Number of images to fetch with each search. */
  private static final int DEFAULT_LIMIT = 100;

  public E621(Context context, String name, String endpoint) {
    super(context, name, endpoint);
  }

  public E621(Context context, String name, String endpoint, String username, String password) {
    super(context, name, endpoint, username, password);
  }

  @Override
  public Settings getSettings() {
    return new Settings(Settings.APIType.E621, name, apiEndpoint);
  }

  @Override
  protected String webUrlFromId(String id) {
    return apiEndpoint + "/post/show/" + id;
  }

  @Override
  protected SearchResult parseXMLResponse(String body, String tags, int offset) throws IOException {

    final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);

    try {
      DocumentBuilderFactory Factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder Builder = Factory.newDocumentBuilder();
      Document doc = Builder.parse(new InputSource(new StringReader(body)));

      NodeList nList = doc.getElementsByTagName("post");

      for(int i = 0; i < nList.getLength(); i++) {

        Node node = nList.item(i);
        if(node.getNodeType() == Node.ELEMENT_NODE) {

          Element element = (Element) node;

          final Image image = new Image();
          image.searchPage = offset;
          image.searchPagePosition = i;


          image.fileUrl = element.getElementsByTagName("file_url").item(0).getTextContent();
          image.width = Integer.parseInt(element.getElementsByTagName("width").item(0).getTextContent());
          image.height = Integer.parseInt(element.getElementsByTagName("height").item(0).getTextContent());

          image.previewUrl = element.getElementsByTagName("preview_url").item(0).getTextContent();
          image.previewWidth = Integer.parseInt(element.getElementsByTagName("preview_width").item(0).getTextContent());
          image.previewHeight = Integer.parseInt(element.getElementsByTagName("preview_height").item(0).getTextContent());

          image.sampleUrl = element.getElementsByTagName("sample_url").item(0).getTextContent();
          image.sampleWidth = Integer.parseInt(element.getElementsByTagName("sample_width").item(0).getTextContent());
          image.sampleHeight = Integer.parseInt(element.getElementsByTagName("sample_height").item(0).getTextContent());

          image.tags = Tag.arrayFromString(element.getElementsByTagName("tags").item(0).getTextContent(), Tag.Type.GENERAL);
          image.id = element.getElementsByTagName("id").item(0).getTextContent();
          image.webUrl = webUrlFromId(image.id);
          image.parentId = element.getElementsByTagName("parent_id").item(0).getTextContent();
          image.safeSearchRating = Image.SafeSearchRating.fromString(element.getElementsByTagName("rating").item(0).getTextContent());
          image.score = Integer.parseInt(element.getElementsByTagName("score").item(0).getTextContent());
          image.md5 = element.getElementsByTagName("md5").item(0).getTextContent();
          image.createdAt = dateFromString(element.getElementsByTagName("created_at").item(0).getTextContent());

          imageList.add(image);
        }
      }

    }
    catch(Exception e) {
      throw new IOException(e);
    }

    return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags), offset);
  }

  /**
   * Create a {@link java.util.Date} object from String date representation used by this API.
   *
   * @param date Date string.
   * @return Date converted from given String.
   */
  @Override
  protected Date dateFromString(String date) throws ParseException {
    final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    // Normalise the ISO8601 time zone into a format parse-able by SimpleDateFormat.
    if (!TextUtils.isEmpty(date)) {
      String newDate = date.replace("Z", "+0000");
      if (newDate.length() == 25) {
        newDate = newDate.substring(0, 22) + newDate.substring(23); // Remove timezone colon.
      }
      return DATE_FORMAT.parse(newDate);
    }

    return null;
  }

}
