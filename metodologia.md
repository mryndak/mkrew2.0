# Metodologia badawcza: Prognozowanie zapotrzebowania na krew w Regionalnych Centrach Krwiodawstwa i Krwiolecznictwa w Polsce

## 1. Cel badania

**Cel główny:** Opracowanie modelu prognostycznego pozwalającego przewidywać zapotrzebowanie na poszczególne grupy krwi w RCKiK, umożliwiającego wczesne wykrywanie potencjalnych niedoborów.

**Cele szczegółowe:**
- Zebranie historycznych danych o stanach magazynowych krwi w poszczególnych RCKiK
- Identyfikacja wzorców sezonowych i cyklicznych w dostępności krwi
- Określenie czynników wpływających na niedobory konkretnych grup krwi
- Stworzenie modelu predykcyjnego dla każdego RCKiK i grupy krwi

## 2. Zakres badania

**Jednostki badawcze:** 21 Regionalnych Centrów Krwiodawstwa i Krwiolecznictwa w Polsce:
- Białystok, Bydgoszcz, Gdańsk, Kalisz, Katowice, Kielce, Kraków, Lublin, Łódź, Olsztyn, Opole, Poznań, Racibórz, Radom, Rzeszów, Słupsk, Szczecin, Wałbrzych, Warszawa, Wrocław, Zielona Góra

**Grupy krwi:** 8 podstawowych grup (A+, A-, B+, B-, AB+, AB-, O+, O-)

**Okres badania:** Minimum 12-24 miesięcy danych historycznych (im dłuższy, tym lepiej)

## 3. Źródła danych

### 3.1. Źródła pierwotne
**Strony internetowe RCKiK** - monitoring stanów magazynowych:
- Codzienne/regularne zbieranie danych o stanach magazynowych publikowanych na stronach RCKiK
- Informacje o pilnych apelach o konkretne grupy krwi
- Komunikaty o wstrzymaniu pobierania określonych grup

**Narzędzia do zbierania:**
- Web scraping (automatyczne pobieranie danych ze stron)
- Ręczne rejestrowanie w przypadku braku strukturyzowanych danych
- Dokumentacja formatu publikacji danych w każdym RCKiK

### 3.2. Źródła uzupełniające
- **Dane kalendarzowe:** dni wolne, święta, wakacje, okres nauki szkolnej
- **Dane epidemiologiczne:** zachorowalność, okresy podwyższonej zachorowalności
- **Dane demograficzne:** liczba ludności w regionie, struktura wiekowa
- **Dane o akcjach krwiodawstwa:** terminy akcji mobilnych, kampanie promujące krwiodawstwo
- **Dane pogodowe:** ekstremalne warunki pogodowe mogące wpływać na dostępność dawców

### 3.3. Potencjalne źródła z współpracy
- Narodowe Centrum Krwi (NCK) - koordynator systemu
- Bezpośredni kontakt z RCKiK w celu uzyskania danych historycznych
- Krajowy Rejestr Dawców Krwi (jeśli dane są dostępne)

## 4. Metodologia zbierania danych

### 4.1. Web scraping stron RCKiK

**Harmonogram zbierania:**
- Codzienne pobieranie danych (najlepiej o tej samej porze, np. 12:00)
- Częstość zwiększona do 2-3 razy dziennie dla dokładniejszego obrazu dynamiki zmian

**Elementy do zbierania:**
- Data i godzina pobrania
- Stan magazynowy dla każdej grupy krwi (jeśli podawany ilościowo)
- Kategoria stanu: niski/średni/zadowalający/wysoki/optymalny
- Komunikaty tekstowe o pilnych potrzebach
- Źródło (konkretne RCKiK)

**Struktura bazy danych:**
```
ID | Data | Godzina | RCKiK | Grupa_krwi | Stan_kategoria | Stan_ilościowy | Uwagi | URL_źródła
```

### 4.2. Kodowanie stanów magazynowych

Standaryzacja kategorii stosowanych przez różne RCKiK:
- Stan niski = 1
- Stan średni = 2
- Stan zadowalający/optymalny = 3
- Stan wysoki = 4

### 4.3. Narzędzia techniczne

**Rekomendowane technologie:**
- **Python** + biblioteki: BeautifulSoup/Scrapy (web scraping), Pandas (analiza danych), Scikit-learn/Prophet (modelowanie)
- **R** - alternatywa z pakietami: rvest, tidyverse, forecast, caret
- **Baza danych:** PostgreSQL lub SQLite do przechowywania danych
- **Harmonogram:** cron (Linux) lub Task Scheduler (Windows) do automatyzacji

## 5. Analiza danych

### 5.1. Analiza eksploracyjna (EDA)

**Statystyki opisowe:**
- Średni stan magazynowy dla każdej grupy krwi w każdym RCKiK
- Częstość występowania niskich stanów
- Czas trwania niedoborów

**Wizualizacje:**
- Serie czasowe stanów magazynowych
- Mapy cieplne pokazujące niedobory w czasie i przestrzeni
- Rozkłady stanów dla poszczególnych grup krwi

### 5.2. Identyfikacja wzorców

**Analiza sezonowości:**
- Wzorce tygodniowe (dni tygodnia)
- Wzorce miesięczne
- Wzorce kwartalne i roczne
- Wpływ świąt i wakacji

**Korelacje:**
- Między różnymi RCKiK
- Między różnymi grupami krwi
- Z czynnikami zewnętrznymi (pogoda, epidemie, kampanie)

### 5.3. Czynniki ryzyka

**Identyfikacja sytuacji kryzysowych:**
- Jakie grupy krwi najczęściej mają niedobory?
- W jakich regionach (RCKiK) problemy występują najczęściej?
- Jakie okresy roku są najbardziej problematyczne?
- Ile czasu trwa przeciętny niedobór?

## 6. Modelowanie predykcyjne

### 6.1. Wybór metod prognostycznych

**Modele do rozważenia:**

**A. Modele statystyczne klasyczne:**
- ARIMA (AutoRegressive Integrated Moving Average) - dla szeregów czasowych
- SARIMA - z uwzględnieniem sezonowości
- Wygładzanie wykładnicze (Exponential Smoothing)

**B. Modele uczenia maszynowego:**
- Random Forest - przewidywanie stanów kategorycznych
- Gradient Boosting (XGBoost, LightGBM)
- LSTM (Long Short-Term Memory) - sieci neuronowe dla szeregów czasowych

**C. Modele dedykowane:**
- Facebook Prophet - specjalnie zaprojektowany dla prognoz biznesowych z sezonowością

**D. Modele ansambowe:**
- Kombinacja kilku modeli dla lepszej dokładności

### 6.2. Zmienne predykcyjne (features)

**Zmienne czasowe:**
- Dzień tygodnia
- Miesiąc
- Kwartał
- Czy dzień świąteczny/przedświąteczny
- Czy okres wakacyjny/szkolny
- Numer tygodnia w roku

**Zmienne opóźnione (lag features):**
- Stan z poprzedniego dnia
- Stan z poprzedniego tygodnia
- Średnia ruchoma z ostatnich 7/14/30 dni

**Zmienne zewnętrzne:**
- Temperatura
- Opady
- Zaplanowane akcje krwiodawstwa
- Kampanie medialne

**Zmienne regionalne:**
- Wielkość populacji obsługiwanej przez RCKiK
- Historyczna częstość niedoborów w danym centrum

### 6.3. Proces budowy modelu

**Etap 1: Przygotowanie danych**
- Oczyszczenie danych (braki, wartości odstające)
- Inżynieria cech (feature engineering)
- Normalizacja/standaryzacja jeśli wymagana

**Etap 2: Podział danych**
- Zbiór treningowy: 70-80% danych (starsze okresy)
- Zbiór walidacyjny: 10-15% (do strojenia hiperparametrów)
- Zbiór testowy: 10-15% (najnowsze dane do ostatecznej oceny)

**Etap 3: Trenowanie modeli**
- Osobny model dla każdego RCKiK lub jeden model uniwersalny z kodowaniem centrum
- Osobne modele dla każdej grupy krwi lub wieloklasowy
- Strojenie hiperparametrów (grid search, random search)

**Etap 4: Ocena modelu**

**Metryki:**
- Dla prognoz kategorycznych (stan niski/średni/wysoki):
  - Accuracy (dokładność)
  - Precision, Recall, F1-score
  - Confusion matrix
  - Szczególnie ważne: wykrywalność stanów niskich (recall dla klasy "niski")

- Dla prognoz ilościowych:
  - MAE (Mean Absolute Error)
  - RMSE (Root Mean Squared Error)
  - MAPE (Mean Absolute Percentage Error)

**Etap 5: Walidacja**
- Walidacja krzyżowa w czasie (time series cross-validation)
- Testowanie na danych "przyszłościowych"

### 6.4. Horyzont prognozy

**Rekomendowane horyzonty:**
- Krótkoterminowy: 1-7 dni (operacyjny)
- Średnioterminowy: 1-4 tygodnie (taktyczny)
- Długoterminowy: 1-3 miesiące (strategiczny)

## 7. System wczesnego ostrzegania

### 7.1. Definicja alertów

**Poziomy alertów:**
- **Poziom 1 (Informacyjny):** Przewidywany spadek do stanu średniego w ciągu 7 dni
- **Poziom 2 (Ostrzeżenie):** Przewidywany spadek do stanu niskiego w ciągu 7 dni
- **Poziom 3 (Krytyczny):** Przewidywany spadek do stanu niskiego w ciągu 3 dni lub bardzo niski stan przez >7 dni

### 7.2. Format rekomendacji

Dla każdego alertu:
- RCKiK
- Grupa krwi
- Przewidywana data niedoboru
- Poziom pewności prognozy
- Rekomendowane działania (np. zwiększenie akcji mobilnych, kampania medialna)

## 8. Harmonogram realizacji projektu

**Faza 1: Przygotowanie (4-6 tygodni)**
- Analiza stron RCKiK i dokumentacja struktury danych
- Stworzenie skryptów do web scrapingu
- Budowa bazy danych
- Uruchomienie codziennego zbierania danych

**Faza 2: Zbieranie danych (3-6 miesięcy minimum)**
- Codzienne gromadzenie danych o stanach magazynowych
- Równoległe pozyskiwanie danych historycznych (jeśli możliwe)
- Zbieranie danych uzupełniających

**Faza 3: Analiza i modelowanie (2-3 miesiące)**
- Analiza eksploracyjna
- Identyfikacja wzorców
- Budowa modeli predykcyjnych
- Testowanie i optymalizacja

**Faza 4: Wdrożenie (1-2 miesiące)**
- Stworzenie systemu generowania prognoz
- Dashboard z wizualizacjami
- System alertów
- Dokumentacja

**Faza 5: Monitorowanie i doskonalenie (ciągłe)**
- Walidacja prognoz vs rzeczywistość
- Doskonalenie modeli
- Raportowanie wyników

## 9. Aspekty etyczne i prawne

### 9.1. Ochrona danych
- Dane są publiczne, ale należy sprawdzić regulaminy stron RCKiK
- Informowanie o wykorzystaniu danych do celów badawczych
- Przestrzeganie RODO przy ewentualnym współdzieleniu danych

### 9.2. Transparentność
- Jasna komunikacja niepewności prognoz
- Wskazanie, że model służy wsparciu decyzji, nie zastępuje ekspertów
- Udostępnienie metodologii

### 9.3. Współpraca
- Możliwość współpracy z Narodowym Centrum Krwi
- Dzielenie się wynikami z RCKiK dla wspólnego dobra
- Potencjalna publikacja wyników (po konsultacji z instytucjami)

## 10. Potencjalne wyzwania i rozwiązania

### 10.1. Brak standaryzacji danych
**Problem:** Różne RCKiK mogą publikować dane w różnych formatach
**Rozwiązanie:** Indywidualne skrypty dla każdego centrum, mapowanie na wspólny format

### 10.2. Niekompletność danych historycznych
**Problem:** Trudność w uzyskaniu długich historii danych
**Rozwiązanie:** Start od zbierania bieżących danych, analiza nawet krótszych szeregów, transfer learning z lepiej udokumentowanych centrów

### 10.3. Jakość prognoz przy rzadkich grupach krwi
**Problem:** Mniej danych dla grup rzadkich (Rh-)
**Rozwiązanie:** Modele hierarchiczne, wykorzystanie informacji z grup powiązanych

### 10.4. Czynniki nieprzewidywalne
**Problem:** Wypadki masowe, epidemie, inne zdarzenia losowe
**Rozwiązanie:** Modele probabilistyczne z przedziałami ufności, system szybkiej aktualizacji prognoz

## 11. Mierniki sukcesu projektu

**Metryki techniczne:**
- Dokładność prognoz >70% dla horyzontu 7-dniowego
- Wykrywalność >80% stanów niskich (szczególnie ważne)
- Średni czas wyprzedzenia alertu: min. 5 dni

**Metryki użyteczności:**
- Redukcja liczby sytuacji kryzysowych
- Lepsza alokacja zasobów (akcje mobilne w odpowiednim czasie i miejscu)
- Pozytywny feedback od RCKiK

## 12. Produkty końcowe

1. **Baza danych** ze stanami magazynowymi RCKiK
2. **Raport analityczny** ze wzorcami i czynnikami ryzyka
3. **Model predykcyjny** z dokumentacją techniczną
4. **Dashboard interaktywny** z wizualizacjami i prognozami
5. **System alertów** dla wczesnego ostrzegania
6. **Dokumentacja metodologiczna** dla powtarzalności
7. **Rekomendacje** dla decydentów

## 13. Dalszy rozwój

**Potencjalne rozszerzenia:**
- Integracja z systemami RCKiK (jeśli nawiążesz współpracę)
- Dodanie prognoz dla składników krwi (osocze, płytki)
- Optymalizacja logistyki transportu między centrami
- Aplikacja mobilna dla krwiodawców z informacją o potrzebach
- Model rekomendacji dla optymalnego planowania akcji krwiodawstwa

---

## Podsumowanie

Ta metodologia zapewnia kompleksowe podejście do problemu przewidywania niedoborów krwi w Polsce. Kluczem do sukcesu będzie:
1. Systematyczne i długoterminowe zbieranie danych
2. Staranna analiza wzorców i czynników
3. Wybór odpowiednich metod modelowania
4. Praktyczna użyteczność wyników (system alertów)
5. Ewentualna współpraca z instytucjami systemu krwiodawstwa

Projekt ma ogromny potencjał społeczny - może realnie pomóc w ratowaniu życia poprzez lepsze zarządzanie zasobami krwi w Polsce.