package co.gongzh.snail.util;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class FlyweightList<T> implements Iterable<T>, Cloneable, Serializable {

	private static final long serialVersionUID = -6583616852359179298L;
	
	private int size;
	private final List<Range> ranges;
	private final List<T> values;
	
	public FlyweightList() {
		size = 0;
		ranges = new ArrayList<Range>();
		values = new ArrayList<T>();
	}
	
	public void add(Range range, T value) {
		if (range.offset < 0 || range.length < 0 || range.offset > size) {
			throw new IndexOutOfBoundsException();
		}
		if (range.length == 0) return;
		
		// find first affected range
		int index = findLastRangeBeforeOffset(range.offset) + 1;
		
		if (index < ranges.size()) {
			// split first range if needed
			Range first = ranges.get(index);
			if (range.offset > first.offset) {
				int len1 = range.offset - first.offset;
				int len2 = first.length - len1;
				first.length = len1;
				Range newRange = Range.make(range.offset, len2);
				ranges.add(index + 1, newRange);
				values.add(index + 1, values.get(index));
				index++;
			}
			
			// shift following ranges
			for (; index < ranges.size(); index++) {
				Range r = ranges.get(index);
				r.offset += range.length;
			}
		}
		
		size += range.length;
		if (value != null) set(range, value);
	}
	
	public void insert(int offset, FlyweightList<T> list) {
		if (offset < 0 || offset > size) {
			throw new IndexOutOfBoundsException();
		}
		if (list.size == 0) return;
		if (list == this) list = subList(Range.make(0, size));
		
		int right = 0;
		final Iterator<Range> rit = list.ranges.iterator();
		Range range = rit.hasNext() ? rit.next() : null;
		final Iterator<T> vit = list.values.iterator();
		T value = vit.hasNext() ? vit.next() : null;
		while (right < list.size) {
			if (range == null) {
				this.add(Range.make(offset, list.size - right), null);
				return;
			} else if (right < range.offset) {
				this.add(Range.make(offset, range.offset - right), null);
				offset += range.offset - right;
				right = range.offset;
			}
			this.add(Range.make(offset, range.length), value);
			offset += range.length;
			right += range.length;
			range = rit.hasNext() ? rit.next() : null;
			value = vit.hasNext() ? vit.next() : null;
		}
	}
	
	public void remove(Range range) {
		if (range.offset < 0 || range.length < 0 || range.offset + range.length > size) {
			throw new IndexOutOfBoundsException();
		}
		if (range.length == 0) return;
		
		// find first affected range
		int index = findLastRangeBeforeOffset(range.offset) + 1;
		
		if (index < ranges.size()) {
			// split first range if needed
			Range first = ranges.get(index);
			if (range.offset > first.offset) {
				int len1 = range.offset - first.offset;
				int len2 = first.length - len1;
				first.length = len1;
				Range newRange = Range.make(range.offset, len2);
				ranges.add(index + 1, newRange);
				values.add(index + 1, values.get(index));
				index++;
			}
			
			// delete following ranges
			final int right = range.offset + range.length;
			while (index < ranges.size()) {
				Range r = ranges.get(index);
				if (r.offset + r.length <= right) {
					// remove this range
					ranges.remove(index);
					values.remove(index);
				} else if (r.offset < right) {
					// cut this range
					r.length -= right - r.offset;
					r.offset = right;
					break;
				} else {
					break;
				}
			}
			
			// prepare for merging
			Range head = index - 1 >= 0 ? ranges.get(index - 1) : null;
			
			// shift rest ranges
			for (; index < ranges.size(); index++) {
				Range r = ranges.get(index);
				r.offset -= range.length;
				
				// merge
				if (head != null) {
					if (head.offset + head.length == r.offset &&
						values.get(index - 1).equals(values.get(index))) {
						head.length += r.length;
						ranges.remove(index);
						values.remove(index);
						index--;
					}
					head = null;
				}
			}
		}
		
		size -= range.length;
	}
	
	public void clear() {
		size = 0;
		ranges.clear();
		values.clear();
	}
	
	public T get(int offset) {
		if (offset < 0 || offset >= size) {
			throw new IndexOutOfBoundsException();
		}
		
		// find first affected range
		int index = findLastRangeBeforeOffset(offset) + 1;
		if (index < ranges.size()) {
			Range range = ranges.get(index);
			if (offset >= range.offset) {
				return values.get(index);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public T get(int offset, Range out_range) {
		if (offset < 0 || offset >= size) {
			throw new IndexOutOfBoundsException();
		}
		
		// find first affected range
		int index = findLastRangeBeforeOffset(offset) + 1;
		if (index < ranges.size()) {
			Range range = ranges.get(index);
			if (offset >= range.offset) {
				if (out_range != null) {
					out_range.offset = range.offset;
					out_range.length = range.length;
				}
				return values.get(index);
			} else if (index > 0) {
				Range prev = ranges.get(index - 1);
				if (out_range != null) {
					out_range.offset = prev.offset + prev.length;
					out_range.length = range.offset - out_range.offset;
				}
				return null;
			} else {
				if (out_range != null) {
					out_range.offset = 0;
					out_range.length = range.offset;
				}
				return null;
			}
		} else if (ranges.size() > 0) {
			Range last = ranges.get(ranges.size() - 1);
			if (out_range != null) {
				out_range.offset = last.offset + last.length;
				out_range.length = size - out_range.offset;
			}
			return null;
		} else {
			if (out_range != null) {
				out_range.offset = 0;
				out_range.length = size;
			}
			return null;
		}
	}
	
	public void set(Range range, T value) {
		if (range.offset < 0 || range.length < 0 || range.offset + range.length > size) {
			throw new IndexOutOfBoundsException();
		}
		if (range.length == 0) return;
		
		// find first affected range
		int index = findLastRangeBeforeOffset(range.offset) + 1;
		
		if (index < ranges.size()) {
			// split first range if needed
			Range first = ranges.get(index);
			if (range.offset > first.offset) {
				int len1 = range.offset - first.offset;
				int len2 = first.length - len1;
				first.length = len1;
				Range newRange = Range.make(range.offset, len2);
				ranges.add(index + 1, newRange);
				values.add(index + 1, values.get(index));
				index++;
			}
			
			// delete following ranges
			final int right = range.offset + range.length;
			while (index < ranges.size()) {
				Range r = ranges.get(index);
				if (r.offset + r.length <= right) {
					// remove this range
					ranges.remove(index);
					values.remove(index);
				} else if (r.offset < right) {
					// cut this range
					r.length -= right - r.offset;
					r.offset = right;
					break;
				} else {
					break;
				}
			}
		}
		
		// set value
		if (value != null) {
			range = (Range) range.clone();
			ranges.add(index, range);
			values.add(index, value);
			
			// merge head
			Range head = index - 1 >= 0 ? ranges.get(index - 1) : null;
			if (head != null && head.offset + head.length == range.offset &&
				values.get(index - 1).equals(values.get(index))) {
				head.length += range.length;
				ranges.remove(index);
				values.remove(index);
				index--;
			}
			
			// merge tail
			range = ranges.get(index);
			Range tail = index + 1 < ranges.size() ? ranges.get(index + 1) : null;
			if (tail != null && range.offset + range.length == tail.offset &&
				values.get(index).equals(values.get(index + 1))) {
				range.length += tail.length;
				ranges.remove(index + 1);
				values.remove(index + 1);
			}
		}
	}
	
	public void set(FlyweightList<T> list) {
		if (list == this) return;
		clear();
		size = list.size;
		for (Range r : list.ranges) {
			ranges.add((Range) r.clone());
		}
		values.addAll(list.values);
	}

	public int size() {
		return size;
	}
	
	public boolean hasNonNullValues() {
		return values.size() > 0;
	}
	
	public int indexOf(Object o) {
		if (o == null) {
			int right = 0;
			Iterator<Range> rit = ranges.iterator();
			Range range = rit.hasNext() ? rit.next() : null;
			while (right < size) {
				if (range == null) return right;
				else if (right < range.offset) return right;
				else {
					right = range.offset + range.length;
					range = rit.hasNext() ? rit.next() : null;
				}
			}
			return -1;
		} else {
			int vindex = values.indexOf(o);
			if (vindex != -1) {
				return ranges.get(vindex).offset;
			} else {
				return -1;
			}
		}
	}

	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			
			int index = 0;
			final Iterator<Range> rit = FlyweightList.this.ranges.iterator();
			final Iterator<T> vit = FlyweightList.this.values.iterator();
			Range range = rit.hasNext() ? rit.next() : null;
			T value = vit.hasNext() ? vit.next() : null;

			@Override
			public boolean hasNext() {
				return index < FlyweightList.this.size;
			}

			@Override
			public T next() {
				if (index < FlyweightList.this.size) {
					if (range == null) {
						index++;
						return null;
					} else if (index < range.offset) {
						index++;
						return null;
					} else if (index < range.offset + range.length) {
						index++;
						return value;
					} else {
						range = rit.hasNext() ? rit.next() : null;
						value = vit.hasNext() ? vit.next() : null;
						return next();
					}
				} else {
					return null;
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	public FlyweightList<T> subList(Range range) {
		if (range.offset < 0 || range.length < 0 || range.offset + range.length > size) {
			throw new IndexOutOfBoundsException();
		}
		if (range.length == 0) return new FlyweightList<T>();
		
		// clone
		FlyweightList<T> list = new FlyweightList<T>();
		list.size = this.size;
		for (Range r : this.ranges) {
			list.ranges.add((Range) r.clone());
		}
		list.values.addAll(this.values);
		
		// clip
		list.remove(Range.make(range.offset + range.length, size - range.offset - range.length));
		list.remove(Range.make(0, range.offset));
		return list;
	}
	
	@Override
	public Object clone() {
		FlyweightList<T> list = new FlyweightList<T>();
		list.size = this.size;
		for (Range range : this.ranges) {
			list.ranges.add((Range) range.clone());
		}
		list.values.addAll(this.values);
		return list;
	}

	@Override
	public int hashCode() {
		return ranges.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FlyweightList) {
			FlyweightList<?> list = (FlyweightList<?>) obj;
			return list.size == this.size &&
					list.ranges.equals(this.ranges) &&
					list.values.equals(this.values);
		} else {
			return false;
		}
	}
	
	private int findLastRangeBeforeOffset(int offset) {
		if (ranges.size() == 0) return -1;
		else return findLastRangeBeforeOffset(offset, 0, ranges.size() - 1);
	}
	
	private int findLastRangeBeforeOffset(int offset, int begin, int end) {
		final int mid = (begin + end) / 2;
		Range range = ranges.get(mid);
		int right = range.offset + range.length;
		if (mid == begin) {
			if (offset > right) {
				if (mid == end) return mid;
				else {
					range = ranges.get(end);
					right = range.offset + range.length;
					return offset >= right ? end : mid;
				}
			} else if (offset < right) {
				return -1;
			} else {
				return mid;
			}
		} else {
			if (offset > right) {
				return findLastRangeBeforeOffset(offset, mid, end);
			} else if (offset < right) {
				return findLastRangeBeforeOffset(offset, begin, mid - 1);
			} else {
				return mid;
			}
		}
	}

}
