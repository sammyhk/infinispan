package org.infinispan.query.impl;

import java.util.List;

import net.jcip.annotations.NotThreadSafe;

/**
 * This is the implementation class for the interface ResultIterator. It is what is
 * returned when the {@link org.infinispan.query.CacheQuery#iterator()} using
 * a {@link org.infinispan.query.FetchOptions.FetchMode#EAGER}.
 * <p/>
 *
 * @author Navin Surtani
 * @author Marko Luksa
 * @deprecated Since 11.0 with no replacement.
 */
@NotThreadSafe
@Deprecated
final class EagerIterator<E> extends AbstractIterator<E> {

   private final List<E> hits;

   EagerIterator(List<E> hits, int fetchSize) {
      super(0, hits.size() - 1, fetchSize);
      this.hits = hits;
   }

   @Override
   public void close() {
      // This method does not need to do anything for this type of iterator as when an instance of it is
      // created, the iterator() method in CacheQueryImpl closes everything that needs to be closed.
   }

   @Override
   protected E loadHit(int index) {
      return hits.get(index);
   }
}
