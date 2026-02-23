package org.okapi.abstractio;

import java.io.IOException;
import org.okapi.pages.AppendOnlyPage;
import org.okapi.streams.StreamIdentifier;

public interface PageWriter<P extends AppendOnlyPage<?, S, M, B>, S, M, B, Id> {
  int appendPage(StreamIdentifier<Id> streamIdentifier, P page) throws IOException;
}
