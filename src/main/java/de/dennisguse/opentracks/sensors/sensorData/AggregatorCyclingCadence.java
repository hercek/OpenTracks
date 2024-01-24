package de.dennisguse.opentracks.sensors.sensorData;

import android.util.Log;

import androidx.annotation.NonNull;
import java.time.Duration;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingCadence;
import de.dennisguse.opentracks.sensors.UintUtils;

public class AggregatorCyclingCadence extends Aggregator<BluetoothHandlerCyclingCadence.CrankData, Cadence> {
    private long startCrankRevolutionCount;
    private int startCrankRevolutionTime; // unit is 1s/10024
    private final String TAG = AggregatorCyclingCadence.class.getSimpleName();

    public AggregatorCyclingCadence(String sensorAddress, String sensorName) {
        super(sensorAddress, sensorName);
        startCrankRevolutionCount = 0;
        startCrankRevolutionTime = -1;
    }

    @Override
    protected void computeValue(Raw<BluetoothHandlerCyclingCadence.CrankData> current) {
        if (!isRecent()) {
            startCrankRevolutionCount = current.value().crankRevolutionsCount();
            startCrankRevolutionTime = 0;
            return;
        }
        if (startCrankRevolutionTime <= -1) {
            // This is 1st valid crank data in the current track point; previous crank data are valid as well.
            // For the start value: use the last crank data which started in the previous track point and will
            // be finished in this track point. This makes the average precise but slightly shifted in time.
            startCrankRevolutionCount = previous.value().crankRevolutionsCount();
            startCrankRevolutionTime = 0;
        }
        // Time is in units of 1s/1024 and it does overflow about every minute (represented in UINT16).
        // Therefore we must always accumulate the differences and cannot just store the original value after reset.
        // Storing the value (as startCrankRevolutionCount) would misbehave when track point gets longer than 32s.
        startCrankRevolutionTime += UintUtils.diff(
                current.value().crankRevolutionsTime(), previous.value().crankRevolutionsTime(), UintUtils.UINT16_MAX );
        // Revolutions can overflow. It happens very rarely (type is UINT32). UintUtils.diff will take care of this.
        long crankDiff = UintUtils.diff(current.value().crankRevolutionsCount(), startCrankRevolutionCount, UintUtils.UINT32_MAX);
        var newValue = Cadence.of( crankDiff, Duration.ofMillis(Math.round(startCrankRevolutionTime*(1000.0f/1024))) );
        // cadence sensors can return garbage sometimes; recover from implausible cadence values
        if (newValue.getRPM() <= 240) value = newValue;
        else Log.e(TAG, "Crank data are invalid: skipping this cadence computation.");
    }

    @Override
    public void reset() {
        startCrankRevolutionCount = 0;
        startCrankRevolutionTime = -1;
    }

    @NonNull
    @Override
    protected Cadence getNoneValue() {
        return Cadence.of(0);
    }
}
