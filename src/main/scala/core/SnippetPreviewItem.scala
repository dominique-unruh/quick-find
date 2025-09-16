package de.unruh.quickfind
package core

import org.apache.commons.text.StringEscapeUtils.escapeHtml4

trait SnippetPreviewItem(preview: Option[(String,String,String)]) extends ChildItem {
  override def previewLine: String = preview match
    case Some((prefix,text,suffix)) => 
      s"<html>${escapeHtml4(prefix)}<u>${escapeHtml4(text)}</u>${escapeHtml4(suffix)}"
    case None =>
      ""
}
