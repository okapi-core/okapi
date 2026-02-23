package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.okapi.abstractfilter.AndPageFilter;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.byterange.RangeIterationException;

public class AndPageFilterTests {

  @Test
  void testOrFilterShouldRead() throws RangeIterationException {
    var left = mock(PageFilter.class);
    var right = mock(PageFilter.class);
    var orFilter = new AndPageFilter<>(left, right);
    when(left.shouldReadPage(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    when(right.shouldReadPage(org.mockito.ArgumentMatchers.any())).thenReturn(false);
    var pageMeta = mock(Object.class);
    assertFalse(orFilter.shouldReadPage(pageMeta));
  }

  @Test
  void testOrFilterShouldRead_rightIsTrue() throws RangeIterationException {
    var left = mock(PageFilter.class);
    var right = mock(PageFilter.class);
    var orFilter = new AndPageFilter<>(left, right);
    when(left.shouldReadPage(org.mockito.ArgumentMatchers.any())).thenReturn(false);
    when(right.shouldReadPage(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    var pageMeta = mock(Object.class);
    assertFalse(orFilter.shouldReadPage(pageMeta));
  }

  @Test
  void testOrFilterShouldSkip() throws RangeIterationException {
    var left = mock(PageFilter.class);
    var right = mock(PageFilter.class);
    var orFilter = new AndPageFilter<>(left, right);
    when(left.shouldReadPage(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    when(right.shouldReadPage(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    var pageMeta = mock(Object.class);
    assertTrue(orFilter.shouldReadPage(pageMeta));
  }
}
