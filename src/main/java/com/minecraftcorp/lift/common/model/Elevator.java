package com.minecraftcorp.lift.common.model;

import com.minecraftcorp.lift.common.exception.ElevatorChangeException;
import com.minecraftcorp.lift.common.exception.ElevatorException;
import com.minecraftcorp.lift.common.exception.ElevatorUsageException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Elevator {

	private static final Messages messages = Messages.INSTANCE;

	protected final List<Floor> floors;
	private final Floor startFloor;
	private Floor destFloor;
	private FloorSign initialSign;

	private final double speed;
	private long startTime;
	private long maxEndTime;

	public Elevator(List<Floor> floors, Floor startFloor, double speed, FloorSign initialSign) {
		this.floors = floors;
		this.startFloor = startFloor;
		this.speed = speed;
		this.initialSign = initialSign;
	}

	public abstract Config getConfig();

	public Optional<Floor> getFloorFromY(int buttonY) {
		return floors.stream()
				.filter(floor -> floor.getButtonY() == buttonY)
				.findFirst();
	}

	public boolean isGoingUp() {
		return getStartFloor().getLevel() < getDestFloor().getLevel();
	}

	public void setDestFloorFromSign() {
		destFloor = getFloorByLevel(getInitialSign().readDestLevel());
	}

	public Optional<Floor> getNextFloor(Floor currentFloor, Floor exempt) {
		if (floors.size() == 1) {
			return Optional.empty();
		}
		int currentLevel = currentFloor.getLevel();
		Floor next = currentLevel < floors.size() ? getFloorByLevel(currentLevel + 1) : getFloorByLevel(1);
		if (next.equals(exempt)) {
			return getNextFloor(next, exempt);
		}
		return Optional.of(next);
	}

	public Optional<Floor> getPreviousFloor(Floor currentFloor, Floor exempt) {
		if (floors.size() == 1) {
			return Optional.empty();
		}
		int currentLevel = currentFloor.getLevel();
		Floor previous = currentLevel > 1 ? getFloorByLevel(currentLevel - 1) : getFloorByLevel(floors.size());
		if (previous.equals(exempt)) {
			return getPreviousFloor(previous, exempt);
		}
		return Optional.of(previous);
	}

	public void initTimeMeasures() {
		startTime = System.currentTimeMillis();
		long rideDuration = (long) (Math.abs(startFloor.getFloorY() - getDestFloor().getFloorY()) * 0.05 * 1000L / speed);
		maxEndTime = startTime + rideDuration + getConfig().getSecondsUntilTimeout() * 1000;
	}

	public Floor getFloorBySign(FloorSign floorSign) {
		Optional<Floor> floor = floors.stream()
				.filter(f -> f.getSigns().contains(floorSign))
				.findFirst();
		if (floor.isEmpty()) {
			throw new ElevatorChangeException("Could not find floor that belongs to clicked floor sign");
		}
		return floor.get();
	}

	public Floor getFloorByLevel(int level) {
		if (level > floors.size()) {
			// Fallback to next floor if level doesn't exist
			Optional<Floor> next = getNextFloor(startFloor, startFloor);
			if (next.isEmpty()) {
				throw new ElevatorException(messages.getFloorNotExists() + level);
			}
			return next.get();
		}
		Optional<Floor> floor = floors.stream()
				.filter(f -> f.getLevel() == level)
				.findFirst();
		if (floor.isEmpty()) {
			throw new ElevatorUsageException(messages.getFloorNotExists() + level);
		}
		return floor.get();
	}

	public List<Floor> getFloorsToRemove() {
		boolean goingUp = isGoingUp();
		Predicate<Floor> select = floor -> goingUp ? floor.compareTo(startFloor) > 0 && floor.compareTo(destFloor) <= 0
				: floor.compareTo(startFloor) <= 0 && floor.compareTo(destFloor) > 0;
		return floors.stream()
				.filter(select)
				.collect(Collectors.toList());
	}
}
