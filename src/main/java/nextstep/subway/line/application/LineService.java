package nextstep.subway.line.application;

import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.domain.Section;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LineService {
    public static final int ZERO = 0;
    public static final int ONE = 1;
    private LineRepository lineRepository;
    private StationService stationService;

    public LineService(LineRepository lineRepository, StationService stationService) {
        this.lineRepository = lineRepository;
        this.stationService = stationService;
    }

    public LineResponse saveLine(LineRequest request) {
        Station upStation = stationService.findById(request.getUpStationId());
        Station downStation = stationService.findById(request.getDownStationId());
        Line persistLine = lineRepository.save(Line.of(request, upStation, downStation));
        return LineResponse.of(persistLine);
    }

    public List<LineResponse> findLines() {
        List<Line> persistLines = lineRepository.findAll();
        return persistLines.stream()
                .map(LineResponse::of)
                .collect(Collectors.toList());
    }

    public List<Section> getAllSections() {
        List<Line> lines = lineRepository.findAll();
        List<Section> results = new ArrayList<>();
        lines.stream()
                .map(Line::getSections)
                .forEach(results::addAll);
        return results;
    }

    public Line findLineById(Long id) {
        return lineRepository.findById(id).orElseThrow(RuntimeException::new);
    }


    public LineResponse findLineResponseById(Long id) {
        Line persistLine = findLineById(id);
        return LineResponse.of(persistLine);
    }

    public void updateLine(Long id, LineRequest lineUpdateRequest) {
        Line persistLine = lineRepository.findById(id).orElseThrow(RuntimeException::new);
        persistLine.update(new Line(lineUpdateRequest.getName(), lineUpdateRequest.getColor()));
    }

    public void deleteLineById(Long id) {
        lineRepository.deleteById(id);
    }

    public void addLineStation(Long lineId, SectionRequest request) {
        Line line = findLineById(lineId);
        Station upStation = stationService.findStationById(request.getUpStationId());
        Station downStation = stationService.findStationById(request.getDownStationId());
        line.addSection(upStation, downStation, request.getDistance());
    }

    public void removeLineStation(Long lineId, Long stationId) {
        Line line = findLineById(lineId);
        Station station = stationService.findStationById(stationId);

        line.removeLineStation(station);
    }

    public Set<Station> getAllStations() {
        List<Section> sections = getAllSections();
        Set<Station> stations = new HashSet<>();
        sections.forEach(it -> {
            stations.add(it.getUpStation());
            stations.add(it.getDownStation());
        });
        return stations;
    }

    public Integer getMaxExtraFee(List<Station> stations) {
        List<Line> lines = lineRepository.findAll();
        List<Section> sections = createSections(stations);
        Set<Line> results = new HashSet<>();

        for(Section section: sections) {
            results = findLinesIncludeSection(lines, section);
        }

        return results.stream()
                .mapToInt(Line::getExtraFee)
                .max()
                .orElse(ZERO);
    }

    private Set<Line> findLinesIncludeSection(List<Line> lines, Section section) {
        Set<Line> results = new HashSet<>();
        lines.stream()
                .filter(it -> it.hasSection(section))
                .findFirst()
                .ifPresent(results::add);
        return results;
    }

    private List<Section> createSections(List<Station> stations) {
        List<Section> sections = new ArrayList<>();
        for (int i = ZERO; i < stations.size() - ONE; i++) {
            Station upStation = stations.get(i);
            Station downStation = stations.get(i + ONE);
            sections.add(new Section(upStation, downStation));
        }
        return sections;
    }
}
