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
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import irt.components.beans.calibration.ProfileTableDetails;
import irt.components.beans.calibration.ProfileTableTypes;
import irt.components.beans.calibration.update.TableValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import irt.components.beans.calibration.ProfileTable;

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

	private Optional<Path> getPath() throws IOException {

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

		oPath = Optional.ofNullable(arPath.get());
		return oPath;
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
}
