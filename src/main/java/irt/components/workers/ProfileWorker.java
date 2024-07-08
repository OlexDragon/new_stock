package irt.components.workers;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.components.beans.WindowsShortcut;
import irt.components.beans.irt.calibration.CalibrationTable;
import irt.components.beans.irt.calibration.PowerDetectorSource;
import irt.components.beans.irt.calibration.ProfileTable;
import irt.components.beans.irt.calibration.ProfileTableDetails;
import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.update.TableValue;
import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ProfileWorker {
	private final static Logger logger = LogManager.getLogger();

	private final List<Path> searchIn = new ArrayList<>();
	private final String serialNumber;

	private Optional<Path> oPath = Optional.empty();

	public ProfileWorker(String profileRootFolder, String serialNumber) {
		logger.traceEntry("{}; {}", profileRootFolder, serialNumber);

		this.serialNumber = Optional.of(serialNumber)
				.filter(
						sn->{
							final char charAt = sn.charAt(0);
							return charAt>='0' && charAt<='9';
						})
				.map("IRT-"::concat)	// if starts from number add "IRT-"
				.orElse(serialNumber);
		Optional.of(profileRootFolder)
		.ifPresent(
				root->{
					searchIn.add(Paths.get(root));
					final File[] listFiles = new File(root).listFiles((dir, name)->name.endsWith(".lnk"));
					Arrays.stream(listFiles)
					.forEach(
							f->{
								try {

									final WindowsShortcut windowsShortcut = new WindowsShortcut(f);
									if(windowsShortcut.isDirectory())
										searchIn.add(windowsShortcut.getPath());

								} catch (IOException | ParseException e) {
									logger.catching(e);
								}
							});
				});
	}

	public ProfileWorker(Path path) {
		oPath = Optional.of(path);
		serialNumber = path.getFileName().toString().split("\\.")[0];
	}

	public boolean exists() throws IOException {
		final Optional<Path> oPath = getPath();
		return oPath.isPresent();
	}

	public Optional<Path> getPath() throws IOException {

		if(oPath.isPresent())
			return oPath;

		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*" + serialNumber + ".bin");
    	final AtomicReference<Path> arPath = new AtomicReference<>();
    	final FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				if(attrs.isRegularFile()) {

					Path name = file.getFileName();
					if (matcher.matches(name)) {
						arPath.set(file);
						return FileVisitResult.TERMINATE;
					}
				}

				return FileVisitResult.CONTINUE;
			}
		};

		final int size = searchIn.size();
		for(int i=0; i<size; i++) {
			final Path start = searchIn.get(i);
			Files.walkFileTree(start, visitor);
			if(oPath.isPresent())
				break;
		}

		return logger.traceExit(oPath = Optional.ofNullable(arPath.get()));
	}

	public void reset() {
		oPath = Optional.empty();
	}

	public Optional<ProfileTable> scanForTable(String description) {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();

		return ProfileTableDetails.valueByDescription(description).map(ProfileTableDetails::getNames)
				.map(
						names->{

							logger.debug(names);
							try(final Scanner scanner = new Scanner(path);) {

								while(scanner.hasNextLine()) {
									final String line = scanner.nextLine().replaceFirst("#", "");

									for(String name : names) {

										if(line.startsWith(name + "-lut-"))
											return new ProfileTable(ProfileTableTypes.OLD, name);

										if(line.startsWith("lut-ref")) {
											final String[] split = line.split("\\s+", 4);

											if(split[2].replaceAll("\"", "").equals(name)) {

												final String index = split[1];
												final ProfileTable profileTable = new ProfileTable(ProfileTableTypes.NEW, name);
												profileTable.setIndex(index);
												return logger.traceExit(profileTable);
											}
										}
									}
								}

							} catch (IOException e) {
								logger.catching(e);
							}

							return logger.traceExit(new ProfileTable(ProfileTableTypes.UNKNOWN, null));
						});
	}

	public boolean saveToProfile(ProfileTable profileTable, List<TableValue> values) {
		logger.traceEntry();

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final StringBuilder sb = new StringBuilder();
		final Path path = oPath.get();
		final String table = profileTable.toString(values);
		logger.debug("\n", table);
		boolean addTable = true;

		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(profileTable.match(line)) {
					if(addTable)
						sb.append(table);
					addTable = false;
				}else
					sb.append(line).append("\r\n");
			}

			logger.debug("Save profile: {}", path);
			Files.write(path, sb.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			logger.info("Profile {} saved.", serialNumber);

			return true;

		} catch (IOException e) {
			logger.catching(e);
		}

		return false;
	}

	private DateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm");
	public boolean saveProperty(String properyName, String value) {
		logger.traceEntry();

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final StringBuilder sb = new StringBuilder();
		final Path path = oPath.get();

		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(line.startsWith(properyName)) {
					Calendar cal = Calendar.getInstance();
					sb.append(properyName).append(' ').append(value).append(" \t# ").append("Property changed by Calibration App. - ").append(dateFormat.format(cal.getTime())).append("\r\n");
				}else
					sb.append(line).append("\r\n");
			}

			logger.debug("Save profile: {}", path);
			Files.write(path, sb.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			logger.info("Profile {} saved.", serialNumber);

			return true;

		} catch (IOException e) {
			logger.catching(e);
		}

		return false;
	}

	public Optional<CalibrationTable> getTable(String description) {
		return scanForTable(description).map(this::getTable);
	}

	public CalibrationTable getTable(ProfileTable profileTable) {

		switch(profileTable.getType()) {

		case NEW:
			newTableScan(profileTable);
			break;

		case OLD:
			oldTableScan(profileTable);
			break;

		case KA:
		case UNKNOWN:
		default:
		}

		return profileTable.getCalibrationTable();
	}

	private void newTableScan(ProfileTable profileTable) {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();
		final List<TableValue> table = new ArrayList<>();

		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(line.startsWith("lut-entry")) {

					final String[] split = line.split("\\s+", 5);

					if(split[1].equals(profileTable.getIndex())) {

						final Number input;
						if(split[2].contains("."))
							input = Double.parseDouble(split[2]);
						else
							input = Integer.parseInt(split[2]);

						Number output = Double.parseDouble(split[3]);

						String comment = null;
						if(split.length==5) {
							final String[] c = split[5].split("#", 2);
							if(c.length==2)
								comment = c[1];
						}
						table.add(new TableValue(input, output, comment));
					}
				}
			}

			final CalibrationTable calibrationTable = new CalibrationTable(serialNumber, table);
			profileTable.setCalibrationTable(calibrationTable);

		} catch (IOException e) {
			logger.catching(e);
		}
	}

	private void oldTableScan(ProfileTable profileTable) {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();
		final List<TableValue> table = new ArrayList<>();

		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(line.startsWith(profileTable.getName() + "-lut-entry")) {

					final String[] split = line.split("\\s+", 3);

					final Number input;
					if(split[1].contains("."))
						input = Double.parseDouble(split[1]);
					else
						input = Integer.parseInt(split[1]);

					Number output = Double.parseDouble(split[2]);

					String comment = null;
					if(split.length==3) {
						final String[] c = split[2].split("#", 2);
						if(c.length==2)
							comment = c[1];
					}
					table.add(new TableValue(input, output, comment));
				}
			}

			final CalibrationTable calibrationTable = new CalibrationTable(serialNumber, table);
			profileTable.setCalibrationTable(calibrationTable);

		} catch (IOException e) {
			logger.catching(e);
		}
	}

	public PowerDetectorSource scanForPowerDetectorSource() {
		final String startWith = "power-detector-source";
		return getProperty(startWith).map(s->s.split("\\s+", 3))
				.map(
						a->{

							if(a.length>1)
								return PowerDetectorSource.ON_BOARD_SENSOR;

							final int index = Integer.parseInt(a[0]);

							final PowerDetectorSource[] values = PowerDetectorSource.values();

							if(index>=values.length)
								return PowerDetectorSource.UNDEFINED;

							return values[index];
						}).orElse(PowerDetectorSource.UNDEFINED);
	}

	public Optional<Double> getGain() {
		final String startWith = "zero-attenuation-gain";
		return getProperty(startWith).map(Double::parseDouble);
	}

	public Optional<String> getDescription() {

		final String startWith = "product-description";
		return getProperty(startWith);
	}

	public Optional<String> getPartNumber() {

		final String startWith = "device-part-number";
		return getProperty(startWith);
	}

	public Optional<String> getProperty(final String startWith) {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();

		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(line.startsWith(startWith)) {

					final String value = line.split("\\s+", 2)[1].split("#",2)[0].trim();

					return Optional.of(value);
				}
			}
		} catch (Exception e) {
			logger.catching(e);
		}

		return Optional.empty();
	}

	public List<String> getProperties(String propertyStartsWith) {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();

		List<String> properties = new ArrayList<>();

		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(line.startsWith(propertyStartsWith)) {

					final String[] split = line.split("\\s+", 2);
					if(split[0].equals(propertyStartsWith))
						properties.add(split[1].split("#",2)[0].trim());
				}
			}
		} catch (Exception e) {
			logger.catching(e);
		}
		return properties;
	}

	public Map<String, String> getLinesStartsWith(String... lineStartsWith) {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();

		Map<String, String> lines = new HashMap<>();

		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				for(String s:lineStartsWith) {
					if(line.startsWith(s))
						lines.put(s, line);
				}
				if(lines.size()==lineStartsWith.length)
					break;
			}
		} catch (Exception e) {
			logger.catching(e);
		}
		return lines;
	}
}
