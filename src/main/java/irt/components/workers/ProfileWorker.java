package irt.components.workers;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.components.beans.irt.calibration.CalibrationTable;
import irt.components.beans.irt.calibration.PowerDetectorSource;
import irt.components.beans.irt.calibration.ProfileTable;
import irt.components.beans.irt.calibration.ProfileTableDetails;
import irt.components.beans.irt.calibration.ProfileTableTypes;
import irt.components.beans.irt.update.TableValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @Getter @ToString
public class ProfileWorker {
	private final static Logger logger = LogManager.getLogger();

	private final String profileRootFolder;
	private final String serialNumber;

	private Optional<Path> oPath = Optional.empty();

	public boolean exists() throws IOException {
		final Optional<Path> oPath = getPath();
		return oPath.isPresent();
	}

	public Optional<Path> getPath() throws IOException {

		if(oPath.isPresent())
			return oPath;

		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + serialNumber + ".bin");
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
		Files.walkFileTree(Paths.get(profileRootFolder), visitor);

		return oPath = Optional.ofNullable(arPath.get());
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
												return profileTable;
											}
										}
									}
								}

							} catch (IOException e) {
								logger.catching(e);
							}

							return new ProfileTable(ProfileTableTypes.UNKNOWN, null);
						});
	}

	public boolean saveToProfile(ProfileTable profileTable, List<TableValue> values) {
		logger.traceEntry();

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final StringBuilder sb = new StringBuilder();
		final Path path = oPath.get();
		final String table = profileTable.toString(values);
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

			Files.write(path, sb.toString().getBytes());

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
						table.add(new TableValue(input, output));
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

					final String[] split = line.split("\\s+", 5);

					final Number input;
					if(split[1].contains("."))
						input = Double.parseDouble(split[1]);
					else
						input = Integer.parseInt(split[1]);

					Number output = Double.parseDouble(split[2]);
					table.add(new TableValue(input, output));
				}
			}

			final CalibrationTable calibrationTable = new CalibrationTable(serialNumber, table);
			profileTable.setCalibrationTable(calibrationTable);

		} catch (IOException e) {
			logger.catching(e);
		}
	}

	public PowerDetectorSource scanForPowerDetectorSource() {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();
		
		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(line.startsWith("power-detector-source")) {

					final String[] split = line.split("#", 2)[0].split("\\s+", 3);

					if(split.length>2)
						return PowerDetectorSource.ON_BOARD_SENSOR;

					final int index = Integer.parseInt(split[1]);

					final PowerDetectorSource[] values = PowerDetectorSource.values();

					if(index>=values.length)
						return PowerDetectorSource.UNDEFINED;

					return values[index];
				}
			}
		} catch (Exception e) {
			logger.catching(e);
		}

		return PowerDetectorSource.UNDEFINED;
	}

	public Optional<Double> getGain() {

		if(!oPath.isPresent())
			throw new RuntimeException("The profile does not exist.");

		final Path path = oPath.get();
		
		try(final Scanner scanner = new Scanner(path);) {

			while(scanner.hasNextLine()) {
				final String line = scanner.nextLine();

				if(line.startsWith("zero-attenuation-gain")) {

					final String[] split = line.split("\\s+", 3);

					return Optional.of(Double.parseDouble(split[1]));
				}
			}
		} catch (Exception e) {
			logger.catching(e);
		}

		return Optional.empty();
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
}
