// DOM Elements
const binaryInput = document.getElementById('binaryInput');
const calculateBtn = document.getElementById('calculateBtn');
const errorMessage = document.getElementById('errorMessage');
const resultsSection = document.getElementById('results');
const stepsSection = document.getElementById('steps');

// Result elements
const originalBinary = document.getElementById('originalBinary');
const originalDecimal = document.getElementById('originalDecimal');
const onesComplement = document.getElementById('onesComplement');
const onesDecimal = document.getElementById('onesDecimal');
const twosComplement = document.getElementById('twosComplement');
const twosDecimal = document.getElementById('twosDecimal');

// Step elements
const stepOriginal = document.getElementById('stepOriginal');
const stepOnes = document.getElementById('stepOnes');
const stepOnesCalc = document.getElementById('stepOnesCalc');
const stepTwos = document.getElementById('stepTwos');

// Event Listeners
calculateBtn.addEventListener('click', handleCalculate);
binaryInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        handleCalculate();
    }
});

binaryInput.addEventListener('input', () => {
    // Clear error message when user starts typing
    hideError();
});

/**
 * Main calculation handler
 */
function handleCalculate() {
    const input = binaryInput.value.trim();
    
    // Validate input
    if (!input) {
        showError('Please enter a binary number');
        return;
    }
    
    if (!isValidBinary(input)) {
        showError('Invalid binary number! Please enter only 0s and 1s.');
        return;
    }
    
    // Remove leading zeros
    const binary = removeLeadingZeros(input) || '0';
    
    // Calculate complements
    const onesComp = calculateOnesComplement(binary);
    const twosComp = calculateTwosComplement(binary);
    
    // Calculate decimal values
    const origDecimal = binaryToDecimal(binary);
    const onesDec = binaryToDecimal(onesComp);
    const twosDec = binaryToDecimal(twosComp);
    
    // Display results
    displayResults(binary, origDecimal, onesComp, onesDec, twosComp, twosDec);
    
    // Display steps
    displaySteps(binary, onesComp, twosComp);
    
    // Hide error if validation passed
    hideError();
}

/**
 * Validates if input is a valid binary number
 */
function isValidBinary(binary) {
    return /^[01]+$/.test(binary);
}

/**
 * Removes leading zeros from binary string
 */
function removeLeadingZeros(binary) {
    let start = 0;
    while (start < binary.length - 1 && binary[start] === '0') {
        start++;
    }
    return binary.substring(start);
}

/**
 * Calculates 1's complement by inverting all bits
 */
function calculateOnesComplement(binary) {
    return binary.split('').map(bit => bit === '0' ? '1' : '0').join('');
}

/**
 * Calculates 2's complement by adding 1 to 1's complement
 */
function calculateTwosComplement(binary) {
    const onesComp = calculateOnesComplement(binary);
    return addBinary(onesComp, '1');
}

/**
 * Adds two binary numbers
 */
function addBinary(binary1, binary2) {
    // Make both strings the same length by padding with zeros
    const maxLength = Math.max(binary1.length, binary2.length);
    binary1 = padBinary(binary1, maxLength);
    binary2 = padBinary(binary2, maxLength);
    
    let result = '';
    let carry = 0;
    
    // Add from right to left (least significant bit first)
    for (let i = maxLength - 1; i >= 0; i--) {
        const bit1 = parseInt(binary1[i]);
        const bit2 = parseInt(binary2[i]);
        const sum = bit1 + bit2 + carry;
        
        result = (sum % 2) + result;
        carry = Math.floor(sum / 2);
    }
    
    // If there's a carry left, add it
    if (carry > 0) {
        result = carry + result;
    }
    
    return result;
}

/**
 * Pads binary string with leading zeros to specified length
 */
function padBinary(binary, length) {
    while (binary.length < length) {
        binary = '0' + binary;
    }
    return binary;
}

/**
 * Converts binary string to decimal (unsigned)
 */
function binaryToDecimal(binary) {
    let decimal = 0;
    let power = 0;
    for (let i = binary.length - 1; i >= 0; i--) {
        if (binary[i] === '1') {
            decimal += Math.pow(2, power);
        }
        power++;
    }
    return decimal;
}

/**
 * Displays calculation results
 */
function displayResults(binary, origDec, onesComp, onesDec, twosComp, twosDec) {
    originalBinary.textContent = binary;
    originalDecimal.textContent = origDec;
    onesComplement.textContent = onesComp;
    onesDecimal.textContent = onesDec;
    twosComplement.textContent = twosComp;
    twosDecimal.textContent = twosDec;
    
    resultsSection.style.display = 'grid';
    
    // Smooth scroll to results
    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/**
 * Displays calculation steps
 */
function displaySteps(binary, onesComp, twosComp) {
    stepOriginal.textContent = binary;
    stepOnes.textContent = onesComp;
    stepOnesCalc.textContent = onesComp;
    stepTwos.textContent = twosComp;
    
    stepsSection.style.display = 'block';
}

/**
 * Shows error message
 */
function showError(message) {
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
    resultsSection.style.display = 'none';
    stepsSection.style.display = 'none';
}

/**
 * Hides error message
 */
function hideError() {
    errorMessage.classList.remove('show');
}

