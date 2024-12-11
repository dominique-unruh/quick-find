package de.unruh.quickfind
package core

import org.apache.commons.text.StringEscapeUtils

trait SnippetPreviewItem(preview: Option[(String,String,String)]) extends Item {
  import StringEscapeUtils.escapeHtml4
  override def previewLine: String = preview match
    case Some((prefix,text,suffix)) => 
      s"<html>${escapeHtml4(prefix)}<u>${escapeHtml4(text)}</u>${escapeHtml4(suffix)}"
    case None =>
      ""
}
