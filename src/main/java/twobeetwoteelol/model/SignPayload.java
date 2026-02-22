package twobeetwoteelol.model;

public record SignPayload(
    String dimension,
    int x,
    int y,
    int z,
    String[] frontLines
) {
}
