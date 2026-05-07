# Mobile Recharge System

A complete Java-based Mobile Recharge System with a modern responsive browser UI and a dependency-free Java backend.

## Features

- Mobile number, customer name, operator, plan, amount, payment method, and balance inputs
- Java backend validation for required fields, 10 digit mobile numbers, positive recharge amounts, and sufficient balance
- Recharge processing with balance deduction
- Auto-generated transaction IDs
- Date and time on every transaction
- Persistent recharge history saved in `data/recharge-history.csv`
- Searchable and filterable recharge history
- Animated loading state, success modal, receipt section, and dark/light theme toggle

## Project Structure

```text
src/com/rechargeapp/
  Main.java
  http/RechargeHttpServer.java
  model/User.java
  model/Payment.java
  model/Recharge.java
  model/RechargeHistory.java
  model/RechargePlan.java
  model/RechargeRequest.java
  model/Operator.java
  service/RechargeService.java
  util/JsonUtil.java
  util/ValidationException.java
web/
  index.html
  assets/styles.css
  assets/app.js
data/
run.ps1
run.bat
```

## Run

From this folder:

```powershell
.\run.ps1
```

Then open:

```text
http://localhost:8080
```

You can also compile and run manually:

```powershell
$files = Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d out $files
java -cp out com.rechargeapp.Main 8080
```
