package org.metadatacenter.cedar.messaging.resources;

import org.metadatacenter.messaging.model.PersistentUserMessageExtract;
import org.metadatacenter.util.http.PagedListResponse;

import java.util.List;

/**
 * One page of the caller's messages, in CEDAR's body envelope. {@code total}, {@code unread} and
 * {@code notnotified} count all of the caller's messages, as the summary does; {@code totalCount}
 * counts the messages the listing's filter admits, which is what the page walks.
 */
public final class MessagePage extends PagedListResponse {

  private final long total;
  private final long unread;
  private final long notnotified;
  private final List<PersistentUserMessageExtract> messages;

  public MessagePage(long total, long unread, long notnotified, List<PersistentUserMessageExtract> messages,
                     String requestUrl, long totalCount, int limit, int offset) {
    this.total = total;
    this.unread = unread;
    this.notnotified = notnotified;
    this.messages = messages;
    page(requestUrl, totalCount, limit, offset, false);
  }

  public long getTotal() {
    return total;
  }

  public long getUnread() {
    return unread;
  }

  public long getNotnotified() {
    return notnotified;
  }

  public List<PersistentUserMessageExtract> getMessages() {
    return messages;
  }
}
