package gov.nist.isg.archiver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipMemoryArchiver implements FilesArchiver {

	private static final String IMAGE_DZI = "image.dzi";

	private static final Pattern PATTERN = Pattern.compile("image_files\\/(\\d+)\\/(\\d+)_(\\d+).jpg");

	private final Path path;

	private final Set<Item> chunks = new TreeSet<>();

	private final Lock lock = new ReentrantLock();

	public ZipMemoryArchiver(Path path) throws IOException {
		this.path = path;
	}

	@Override
	public void close() throws IOException {
		boolean delete = true;
		try (var zos = new ZipOutputStream(Files.newOutputStream(path))) {
			for (var i : chunks) {
				var entry = new ZipEntry(i.getFileName());
				entry.setSize(i.getData().length);
				entry.setCompressedSize(i.getData().length);
				var crc = new CRC32();
	            crc.update(i.getData());
				entry.setCrc(crc.getValue());
				entry.setMethod(ZipEntry.STORED);
				zos.putNextEntry(entry);				
				zos.write(i.getData());
			}
			delete = false;
		} finally {
			if (delete) {
				Files.deleteIfExists(path);
			}
		}
	}

	@Override
	public <T> T appendFile(String path, FileAppender<T> appender) throws IOException {
		var baos = new ByteArrayOutputStream();
		var ret = appender.append(baos);
		var arr = baos.toByteArray();
		add(path, arr);
		return ret;
	}

	@Override
	public <T> T appendBigFile(String path, FileAppender<T> appender) throws IOException {
		var baos = new ByteArrayOutputStream();
		var ret = appender.append(baos);
		var arr = baos.toByteArray();
		add(path, arr);
		return ret;
	}

	@Override
	public void appendFile(String path, File file) throws IOException {
		var baos = new ByteArrayOutputStream();
		Files.copy(file.toPath(), baos);
		var arr = baos.toByteArray();
		add(path, arr);
	}

	private void add(String path, byte[] data) {
		var correctedPath = path.replaceAll("\\\\", "\\/");
		var matcher = PATTERN.matcher(correctedPath);
		int key1 = -1;
		int key2 = -1;
		int key3 = -1;
		if (!matcher.matches()) {
			if (!IMAGE_DZI.equals(correctedPath)) {
				throw new IllegalArgumentException();
			}
		} else {
			key1 = Integer.parseInt(matcher.group(1));
			key2 = Integer.parseInt(matcher.group(2));
			key3 = Integer.parseInt(matcher.group(3));
		}

		try {
			lock.lock();
			chunks.add(new Item(correctedPath, data, key1, key2, key3));
		} finally {
			lock.unlock();
		}
	}

	private static class Item implements Comparable<Item> {

		private final String fileName;

		private final byte[] data;

		private final int key1;

		private final int key2;

		private final int key3;

		public Item(String fileName, byte[] data, int key1, int key2, int key3) {
			this.fileName = fileName;
			this.data = data;
			this.key1 = key1;
			this.key2 = key2;
			this.key3 = key3;
		}

		public String getFileName() {
			return fileName;
		}

		public byte[] getData() {
			return data;
		}

		@Override
		public int compareTo(Item item) {
			if (key1 < item.key1) {
				return -1;
			} else if (key1 > item.key1) {
				return 1;
			}
			// key1 == item.keey1
			if (key2 < item.key2) {
				return -1;
			} else if (key2 > item.key2) {
				return 1;
			}
			// key2 == item.key2
			if (key3 < item.key3) {
				return -1;
			} else if (key3 > item.key3) {
				return 1;
			}
			return 0;
		}

	}

}
