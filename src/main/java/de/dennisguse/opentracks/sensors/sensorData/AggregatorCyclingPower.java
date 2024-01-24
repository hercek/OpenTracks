package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;
import java.time.Duration;

import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.sensors.BluetoothHandlerManagerCyclingPower;

public class AggregatorCyclingPower extends Aggregator<BluetoothHandlerManagerCyclingPower.Data, Power> {
    private long accumulatedPowerTimeMs;

    public AggregatorCyclingPower(String name, String address) {
        super(name, address);
        accumulatedPowerTimeMs = 0;
    }

    @Override
    public void computeValue(Raw<BluetoothHandlerManagerCyclingPower.Data> current) {
        if (!isRecent()) {
            this.value = current.value().power();
            accumulatedPowerTimeMs = 0;
        } else if (accumulatedPowerTimeMs == 0) {
            this.value = Power.of(current.value().power().getW());
            accumulatedPowerTimeMs = Duration.between(previous.time(), current.time()).toMillis();
        } else {
            final long currentPowerTimeMs = Duration.between(previous.time(), current.time()).toMillis();
            final long nextPowerTimeMs = accumulatedPowerTimeMs + currentPowerTimeMs;
            this.value = Power.of((accumulatedPowerTimeMs * previous.value().power().getW() +
                    currentPowerTimeMs * current.value().power().getW()) / nextPowerTimeMs);
            accumulatedPowerTimeMs = nextPowerTimeMs;
        }
    }

    @Override
    public void reset() {
        accumulatedPowerTimeMs = 0;
    }

    @NonNull
    @Override
    protected Power getNoneValue() {
        return Power.of(0f);
    }
}
